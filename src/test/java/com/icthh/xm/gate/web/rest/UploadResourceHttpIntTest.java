package com.icthh.xm.gate.web.rest;

import com.icthh.xm.commons.security.jwt.TokenProvider;
import com.icthh.xm.commons.security.internal.XmAuthentication;
import com.icthh.xm.gate.GateApp;
import com.icthh.xm.gate.config.AsyncSyncConfiguration;
import com.icthh.xm.gate.config.TestSecurityConfiguration;
import com.icthh.xm.gate.service.file.upload.UploadFileService;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Upload proxy through real Tomcat and real multipart parsing (MockMvc multipart() bypasses multipart resolving).
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = { GateApp.class, TestSecurityConfiguration.class, AsyncSyncConfiguration.class },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "application.enable-upload-resource=true",
        "spring.servlet.multipart.enabled=true",
        // real web server start triggers consul registration, there is no consul on CI
        "spring.cloud.service-registry.auto-registration.enabled=false"
    }
)
class UploadResourceHttpIntTest {

    private static final String SERVICE_NAME = "entity";
    private static final String TOKEN = "test-token";

    @LocalServerPort
    private int port;

    @Autowired
    private UploadFileService uploadFileService;

    @Autowired
    private TokenProvider tokenProvider;

    @MockitoBean
    private DiscoveryClient discoveryClient;

    private HttpServer backend;
    private final AtomicReference<String> backendRequest = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        backend = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        backend.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            backendRequest.set(exchange.getRequestMethod() + " " + exchange.getRequestURI() + "\n" + body);
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        backend.start();

        when(discoveryClient.getInstances(SERVICE_NAME)).thenReturn(List.of(new DefaultServiceInstance(
            "entity-1", SERVICE_NAME, "localhost", backend.getAddress().getPort(), false)));

        // test configuration replaces RestClient with a mock
        ReflectionTestUtils.setField(uploadFileService, "restClient", RestClient.create());
    }

    @AfterEach
    void tearDown() {
        backend.stop(0);
    }

    @Test
    void uploadIsProxiedForAnonymousPath() {
        ResponseEntity<String> response = upload("/upload/entity/api/functions/anonymous/UPLOAD/upload", null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(backendRequest.get()).startsWith("POST /api/functions/anonymous/UPLOAD/upload");
    }

    @Test
    void uploadIsProxiedForAuthenticatedUser() {
        Claims claims = mock(Claims.class);
        XmAuthentication authentication = mock(XmAuthentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(tokenProvider.validateToken(TOKEN)).thenReturn(claims);
        when(tokenProvider.getAuthentication(any(jakarta.servlet.http.HttpServletRequest.class), anyString(), any()))
            .thenReturn(authentication);

        ResponseEntity<String> response = upload("/upload/entity/api/functions/UPLOAD/upload", TOKEN);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(backendRequest.get()).startsWith("POST /api/functions/UPLOAD/upload");
    }

    private ResponseEntity<String> upload(String path, String token) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("test content".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "test.txt";
            }
        });
        return RestClient.create().post()
            .uri("http://localhost:" + port + path)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .headers(headers -> {
                if (token != null) {
                    headers.setBearerAuth(token);
                }
            })
            .body(body)
            .exchange((request, response) -> ResponseEntity.status(response.getStatusCode())
                .body(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
    }
}
