package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.gate.service.TenantMappingService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static com.icthh.xm.gate.config.Constants.HEADER_DOMAIN;
import static com.icthh.xm.gate.config.Constants.HEADER_PORT;
import static com.icthh.xm.gate.config.Constants.HEADER_SCHEME;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static com.icthh.xm.gate.config.Constants.HEADER_WEBAPP_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddDomainRelayHeadersFunctionsUnitTest {

    @Mock
    private ServerRequest serverRequest;

    @Mock
    private HandlerFunction<ServerResponse> next;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private TenantMappingService tenantMappingService;

    @Mock
    private ServerResponse mockResponse;

    @Mock
    private ServerRequest.Headers requestHeaders;

    private HandlerFilterFunction<ServerResponse, ServerResponse> filter;
    private MockedStatic<MvcUtils> mvcUtilsMock;

    @BeforeEach
    void setUp() {
        filter = AddDomainRelayHeadersFunctions.addDomainRelayHeaders();
        when(serverRequest.servletRequest()).thenReturn(servletRequest);
        when(serverRequest.headers()).thenReturn(requestHeaders);
        when(requestHeaders.asHttpHeaders()).thenReturn(new HttpHeaders());
        when(serverRequest.cookies()).thenReturn(new LinkedMultiValueMap<>());
        when(serverRequest.params()).thenReturn(new LinkedMultiValueMap<>());

        when(applicationContext.getBean(TenantMappingService.class)).thenReturn(tenantMappingService);

        mvcUtilsMock = mockStatic(MvcUtils.class);
        mvcUtilsMock.when(() -> MvcUtils.getApplicationContext(serverRequest)).thenReturn(applicationContext);
    }

    @AfterEach
    void tearDown() {
        mvcUtilsMock.close();
    }

    @Test
    void addDomainRelayHeaders_shouldAddAllHeaders() throws Exception {
        when(servletRequest.getServerName()).thenReturn("test.example.com");
        when(servletRequest.getScheme()).thenReturn("https");
        when(servletRequest.getServerPort()).thenReturn(443);
        when(tenantMappingService.getTenantKey("test.example.com")).thenReturn("TEST");
        when(next.handle(any(ServerRequest.class))).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        ArgumentCaptor<ServerRequest> requestCaptor = ArgumentCaptor.forClass(ServerRequest.class);
        verify(next).handle(requestCaptor.capture());

        ServerRequest capturedRequest = requestCaptor.getValue();
        assertEquals("https", capturedRequest.headers().firstHeader(HEADER_SCHEME));
        assertEquals("test.example.com", capturedRequest.headers().firstHeader(HEADER_DOMAIN));
        assertEquals("443", capturedRequest.headers().firstHeader(HEADER_PORT));
        assertEquals("TEST", capturedRequest.headers().firstHeader(HEADER_TENANT));
        assertEquals(mockResponse, response);
    }

    @Test
    void addDomainRelayHeaders_shouldAddWebappUrlFromReferer() throws Exception {
        when(servletRequest.getServerName()).thenReturn("test.example.com");
        when(servletRequest.getScheme()).thenReturn("https");
        when(servletRequest.getServerPort()).thenReturn(443);
        when(servletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("https://webapp.example.com/some/path?query=1");
        when(tenantMappingService.getTenantKey("test.example.com")).thenReturn("TEST");
        when(next.handle(any(ServerRequest.class))).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        ArgumentCaptor<ServerRequest> requestCaptor = ArgumentCaptor.forClass(ServerRequest.class);
        verify(next).handle(requestCaptor.capture());

        ServerRequest capturedRequest = requestCaptor.getValue();
        assertEquals("https://webapp.example.com", capturedRequest.headers().firstHeader(HEADER_WEBAPP_URL));
        assertEquals(mockResponse, response);
    }

    @Test
    void addDomainRelayHeaders_shouldNotAddWebappUrl_whenRefererIsBlank() throws Exception {
        when(servletRequest.getServerName()).thenReturn("test.example.com");
        when(servletRequest.getScheme()).thenReturn("https");
        when(servletRequest.getServerPort()).thenReturn(443);
        when(servletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("");
        when(tenantMappingService.getTenantKey("test.example.com")).thenReturn("TEST");
        when(next.handle(any(ServerRequest.class))).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        ArgumentCaptor<ServerRequest> requestCaptor = ArgumentCaptor.forClass(ServerRequest.class);
        verify(next).handle(requestCaptor.capture());

        ServerRequest capturedRequest = requestCaptor.getValue();
        assertTrue(capturedRequest.headers().header(HEADER_WEBAPP_URL).isEmpty());
        assertEquals(mockResponse, response);
    }

    @Test
    void addDomainRelayHeaders_shouldNotAddWebappUrl_whenRefererIsNull() throws Exception {
        when(servletRequest.getServerName()).thenReturn("test.example.com");
        when(servletRequest.getScheme()).thenReturn("https");
        when(servletRequest.getServerPort()).thenReturn(443);
        when(servletRequest.getHeader(HttpHeaders.REFERER)).thenReturn(null);
        when(tenantMappingService.getTenantKey("test.example.com")).thenReturn("TEST");
        when(next.handle(any(ServerRequest.class))).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        ArgumentCaptor<ServerRequest> requestCaptor = ArgumentCaptor.forClass(ServerRequest.class);
        verify(next).handle(requestCaptor.capture());

        ServerRequest capturedRequest = requestCaptor.getValue();
        assertTrue(capturedRequest.headers().header(HEADER_WEBAPP_URL).isEmpty());
        assertEquals(mockResponse, response);
    }

    @Test
    void addDomainRelayHeaders_shouldHandleInvalidRefererGracefully() throws Exception {
        when(servletRequest.getServerName()).thenReturn("test.example.com");
        when(servletRequest.getScheme()).thenReturn("https");
        when(servletRequest.getServerPort()).thenReturn(443);
        when(servletRequest.getHeader(HttpHeaders.REFERER)).thenReturn("not-a-valid-url");
        when(tenantMappingService.getTenantKey("test.example.com")).thenReturn("TEST");
        when(next.handle(any(ServerRequest.class))).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        ArgumentCaptor<ServerRequest> requestCaptor = ArgumentCaptor.forClass(ServerRequest.class);
        verify(next).handle(requestCaptor.capture());

        assertEquals(mockResponse, response);
    }

    @Test
    void addDomainRelayHeaders_shouldContinueOnException() throws Exception {
        when(servletRequest.getHeader(HttpHeaders.REFERER)).thenThrow(new RuntimeException("Test exception"));

        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }
}
