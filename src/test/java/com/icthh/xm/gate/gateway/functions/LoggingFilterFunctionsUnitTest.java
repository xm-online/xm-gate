package com.icthh.xm.gate.gateway.functions;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingFilterFunctionsUnitTest {

    @Mock
    private ServerRequest serverRequest;

    @Mock
    private HandlerFunction<ServerResponse> next;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private ServerResponse mockResponse;

    private HandlerFilterFunction<ServerResponse, ServerResponse> filter;
    private MockedStatic<MdcUtils> mdcUtilsMock;
    private MockedStatic<MvcUtils> mvcUtilsMock;

    @BeforeEach
    void setUp() {
        filter = LoggingFilterFunctions.addLogging();
        when(serverRequest.servletRequest()).thenReturn(servletRequest);
        mdcUtilsMock = mockStatic(MdcUtils.class);
        mvcUtilsMock = mockStatic(MvcUtils.class);
    }

    @AfterEach
    void tearDown() {
        mdcUtilsMock.close();
        mvcUtilsMock.close();
    }

    @Test
    void addLogging_shouldSkipLogging_forHealthEndpoint() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/management/health");
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        mdcUtilsMock.verify(MdcUtils::generateRid, never());
        assertEquals(mockResponse, response);
    }

    @Test
    void addLogging_shouldLogRequestDetails() throws Exception {
        setupLoggingMocks();
        when(next.handle(serverRequest)).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(HttpStatus.OK);

        mdcUtilsMock.when(MdcUtils::getRid).thenReturn(null);
        mdcUtilsMock.when(MdcUtils::generateRid).thenReturn("test-rid");

        ServerResponse response = filter.filter(serverRequest, next);

        mdcUtilsMock.verify(() -> MdcUtils.putRid("test-rid:testuser:TEST"));
        mdcUtilsMock.verify(MdcUtils::clear);
        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void addLogging_shouldUseExistingRid_whenAvailable() throws Exception {
        setupLoggingMocks();
        when(next.handle(serverRequest)).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(HttpStatus.OK);

        mdcUtilsMock.when(MdcUtils::getRid).thenReturn("existing-rid");

        ServerResponse response = filter.filter(serverRequest, next);

        mdcUtilsMock.verify(() -> MdcUtils.putRid("existing-rid:testuser:TEST"));
        mdcUtilsMock.verify(MdcUtils::clear);
        assertEquals(mockResponse, response);
    }

    @Test
    void addLogging_shouldClearMdc_onException() throws Exception {
        setupLoggingMocks();
        RuntimeException testException = new RuntimeException("Test error");
        when(next.handle(serverRequest)).thenThrow(testException);

        mdcUtilsMock.when(MdcUtils::getRid).thenReturn(null);
        mdcUtilsMock.when(MdcUtils::generateRid).thenReturn("test-rid");

        assertThrows(RuntimeException.class, () -> filter.filter(serverRequest, next));

        mdcUtilsMock.verify(MdcUtils::clear);
    }

    @Test
    void addLogging_shouldLogContentLength() throws Exception {
        setupLoggingMocks();
        when(servletRequest.getContentLengthLong()).thenReturn(1024L);
        when(next.handle(serverRequest)).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(HttpStatus.OK);

        mdcUtilsMock.when(MdcUtils::getRid).thenReturn(null);
        mdcUtilsMock.when(MdcUtils::generateRid).thenReturn("test-rid");

        ServerResponse response = filter.filter(serverRequest, next);

        verify(servletRequest).getContentLengthLong();
        assertEquals(mockResponse, response);
    }

    private void setupLoggingMocks() {
        when(servletRequest.getRequestURI()).thenReturn("/api/test");
        when(servletRequest.getServerName()).thenReturn("test.example.com");
        when(servletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(servletRequest.getContentLengthLong()).thenReturn(0L);
        when(servletRequest.getMethod()).thenReturn("GET");
        when(servletRequest.getRemoteUser()).thenReturn("testuser");

        // Mock MvcUtils.getApplicationContext() to return our mocked ApplicationContext
        mvcUtilsMock.when(() -> MvcUtils.getApplicationContext(serverRequest)).thenReturn(applicationContext);

        when(applicationContext.getBean(TenantContextHolder.class)).thenReturn(tenantContextHolder);
        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));
    }
}
