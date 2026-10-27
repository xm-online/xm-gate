package com.icthh.xm.gate.service;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayServiceInstanceServiceUnitTest {

    private static final int ITERATIONS = 500;

    private final ConcurrentLinkedQueue<String> receivedRequests = new ConcurrentLinkedQueue<>();
    private final ThreadLocal<String> currentUser = new ThreadLocal<>();

    private HttpServer backend;
    private GatewayServiceInstanceService service;

    @BeforeEach
    void setUp() throws Exception {
        backend = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        backend.setExecutor(Executors.newFixedThreadPool(4));
        backend.createContext("/", exchange -> {
            // request path starts with the caller name: /<user>/management/info
            receivedRequests.add(exchange.getRequestURI().getPath().split("/")[1] + "|"
                + exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{\"status\":\"UP\",\"build\":{}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        backend.start();

        XmAuthenticationContextHolder holder = mock(XmAuthenticationContextHolder.class);
        when(holder.getContext()).thenAnswer(invocation -> {
            XmAuthenticationContext context = mock(XmAuthenticationContext.class);
            when(context.getTokenValue()).thenReturn(Optional.of("token-" + currentUser.get()));
            when(context.getTokenType()).thenReturn(Optional.of("Bearer"));
            return context;
        });
        service = new GatewayServiceInstanceService(holder, "");
    }

    @AfterEach
    void tearDown() {
        backend.stop(0);
    }

    @Test
    void concurrentCallersSendOwnTokenToInstanceInfo() throws Exception {
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        CyclicBarrier barrier = new CyclicBarrier(2);

        Thread userA = new Thread(() -> callInstanceInfo("A", barrier, errors));
        Thread userB = new Thread(() -> callInstanceInfo("B", barrier, errors));
        userA.start();
        userB.start();
        userA.join();
        userB.join();

        assertThat(errors).isEmpty();
        assertThat(receivedRequests).hasSize(2 * ITERATIONS);
        assertThat(receivedRequests).allSatisfy(request -> {
            String[] parts = request.split("\\|", 2);
            assertThat(parts[1]).isEqualTo("Bearer token-" + parts[0]);
        });
    }

    @Test
    void managementPortReplacesInstancePortForHealthAndInfo() {
        currentUser.set("M");
        int port = backend.getAddress().getPort();
        GatewayServiceInstanceService withManagementPort = new GatewayServiceInstanceService(
            mockHolder("M"), String.valueOf(port));
        ServiceInstance instance = new DefaultServiceInstance("svc-M", "svc", "localhost", 1, false) {
            @Override
            public URI getUri() {
                return URI.create("http://localhost:1/M");
            }
        };

        assertThat(withManagementPort.receiveServiceStatus(instance)).isEqualTo("UP");
        assertThat(withManagementPort.extractServiceMetaData(List.of(instance), Map.of("http://localhost:1/M", "UP")))
            .containsKey("http://localhost:1/M");
        assertThat(receivedRequests).hasSize(2);
    }

    private XmAuthenticationContextHolder mockHolder(String user) {
        XmAuthenticationContextHolder holder = mock(XmAuthenticationContextHolder.class);
        XmAuthenticationContext context = mock(XmAuthenticationContext.class);
        when(context.getTokenValue()).thenReturn(Optional.of("token-" + user));
        when(context.getTokenType()).thenReturn(Optional.of("Bearer"));
        when(holder.getContext()).thenReturn(context);
        return holder;
    }

    private void callInstanceInfo(String user, CyclicBarrier barrier, ConcurrentLinkedQueue<Throwable> errors) {
        currentUser.set(user);
        URI uri = URI.create("http://localhost:" + backend.getAddress().getPort() + "/" + user);
        ServiceInstance instance = new DefaultServiceInstance("svc-" + user, "svc", "localhost",
            backend.getAddress().getPort(), false) {
            @Override
            public URI getUri() {
                return uri;
            }
        };
        for (int i = 0; i < ITERATIONS; i++) {
            try {
                barrier.await();
                service.extractServiceMetaData(List.of(instance), Map.of(uri.toString(), "UP"));
            } catch (Exception e) {
                errors.add(e);
            }
        }
    }
}
