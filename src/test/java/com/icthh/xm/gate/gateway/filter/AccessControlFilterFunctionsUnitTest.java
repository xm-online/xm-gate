package com.icthh.xm.gate.gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import tech.jhipster.config.JHipsterProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlFilterFunctionsUnitTest {

    @Mock
    private ServerRequest serverRequest;

    @Mock
    private HandlerFunction<ServerResponse> next;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private DiscoveryClient discoveryClient;

    @Mock
    private JHipsterProperties jHipsterProperties;

    @Mock
    private JHipsterProperties.Gateway gateway;

    @Mock
    private ServerResponse mockResponse;

    private HandlerFilterFunction<ServerResponse, ServerResponse> filter;
    private MockedStatic<MvcUtils> mvcUtilsMock;

    @BeforeEach
    void setUp() {
        filter = AccessControlFilterFunctions.accessControl();
        when(serverRequest.servletRequest()).thenReturn(servletRequest);

        when(applicationContext.getBean(DiscoveryClient.class)).thenReturn(discoveryClient);
        when(applicationContext.getBean(JHipsterProperties.class)).thenReturn(jHipsterProperties);

        mvcUtilsMock = mockStatic(MvcUtils.class);
        mvcUtilsMock.when(() -> MvcUtils.getApplicationContext(serverRequest)).thenReturn(applicationContext);
    }

    @AfterEach
    void tearDown() {
        mvcUtilsMock.close();
    }

    @Test
    void accessControl_shouldProceed_whenServiceNameIsEmpty() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/");
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void accessControl_shouldProceed_whenServiceNotRegistered() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/unknown-service/api/test");
        when(discoveryClient.getServices()).thenReturn(List.of("other-service"));
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void accessControl_shouldProceed_whenNoAccessControlConfigured() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/api/test");
        when(discoveryClient.getServices()).thenReturn(List.of("my-service"));
        when(jHipsterProperties.getGateway()).thenReturn(gateway);
        when(gateway.getAuthorizedMicroservicesEndpoints()).thenReturn(null);
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void accessControl_shouldProceed_whenEmptyAccessControlConfigured() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/api/test");
        when(discoveryClient.getServices()).thenReturn(List.of("my-service"));
        when(jHipsterProperties.getGateway()).thenReturn(gateway);
        when(gateway.getAuthorizedMicroservicesEndpoints()).thenReturn(Collections.emptyMap());
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void accessControl_shouldProceed_whenNoPatternForService() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/api/test");
        when(discoveryClient.getServices()).thenReturn(List.of("my-service"));
        when(jHipsterProperties.getGateway()).thenReturn(gateway);
        when(gateway.getAuthorizedMicroservicesEndpoints()).thenReturn(Map.of("other-service", List.of("/api/**")));
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void accessControl_shouldProceed_whenPatternMatches() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/api/test");
        when(discoveryClient.getServices()).thenReturn(List.of("my-service"));
        when(jHipsterProperties.getGateway()).thenReturn(gateway);
        when(gateway.getAuthorizedMicroservicesEndpoints()).thenReturn(Map.of("my-service", List.of("/api/**")));
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void accessControl_shouldReturnForbidden_whenPatternDoesNotMatch() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/admin/settings");
        when(discoveryClient.getServices()).thenReturn(List.of("my-service"));
        when(jHipsterProperties.getGateway()).thenReturn(gateway);
        when(gateway.getAuthorizedMicroservicesEndpoints()).thenReturn(Map.of("my-service", List.of("/api/**")));

        ServerResponse response = filter.filter(serverRequest, next);

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode());
    }

    @Test
    void accessControl_shouldMatchCaseInsensitiveServiceName() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/MY-SERVICE/api/test");
        when(discoveryClient.getServices()).thenReturn(List.of("my-service"));
        when(jHipsterProperties.getGateway()).thenReturn(gateway);
        when(gateway.getAuthorizedMicroservicesEndpoints()).thenReturn(Map.of("MY-SERVICE", List.of("/api/**")));
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void accessControl_shouldExtractServiceName_fromSimplePath() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/service");
        when(discoveryClient.getServices()).thenReturn(List.of("service"));
        when(jHipsterProperties.getGateway()).thenReturn(gateway);
        when(gateway.getAuthorizedMicroservicesEndpoints()).thenReturn(Map.of("service", List.of("/")));
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }
}
