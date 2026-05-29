package com.icthh.xm.gate.gateway.functions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.icthh.xm.gate.config.Constants.HEADER_X_API_KEY;
import static com.icthh.xm.gate.utils.ServerRequestUtils.BEARER_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ApiKeyRestoreFunctionsUnitTest {

    private HandlerFilterFunction<ServerResponse, ServerResponse> filter;

    @BeforeEach
    void setUp() {
        filter = ApiKeyRestoreFunctions.restoreApiKey();
    }

    @Test
    void shouldRestoreAuthorizationHeaderFromApiKey() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HEADER_X_API_KEY, "external-api-key");
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer internal-jwt");

        ServerRequest request = ServerRequest.create(servletRequest, List.of());

        AtomicReference<ServerRequest> capturedRequest = new AtomicReference<>();

        HandlerFunction<ServerResponse> next = serverRequest -> {
            capturedRequest.set(serverRequest);
            return ServerResponse.ok().build();
        };

        filter.filter(request, next);

        String authorization = capturedRequest.get().headers().firstHeader(HttpHeaders.AUTHORIZATION);

        assertThat(authorization).isEqualTo(BEARER_PREFIX + "external-api-key");
    }

    @Test
    void shouldSkipRestoreWhenApiKeyMissing() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer internal-jwt");

        ServerRequest request = ServerRequest.create(servletRequest, List.of());

        AtomicReference<ServerRequest> capturedRequest = new AtomicReference<>();

        HandlerFunction<ServerResponse> next = serverRequest -> {
            capturedRequest.set(serverRequest);
            return ServerResponse.ok().build();
        };

        filter.filter(request, next);

        String authorization = capturedRequest.get().headers().firstHeader(HttpHeaders.AUTHORIZATION);

        assertThat(authorization).isEqualTo("Bearer internal-jwt");
    }

    @Test
    void shouldReplaceExistingAuthorizationHeader() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HEADER_X_API_KEY, "restored-api-key");
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer old-token");

        ServerRequest request = ServerRequest.create(servletRequest, List.of());

        AtomicReference<ServerRequest> capturedRequest = new AtomicReference<>();

        HandlerFunction<ServerResponse> next = serverRequest -> {
            capturedRequest.set(serverRequest);
            return ServerResponse.ok().build();
        };

        filter.filter(request, next);

        List<String> authorizationHeaders = capturedRequest.get().headers().header(HttpHeaders.AUTHORIZATION);

        assertThat(authorizationHeaders).containsExactly("Bearer restored-api-key");
    }
}
