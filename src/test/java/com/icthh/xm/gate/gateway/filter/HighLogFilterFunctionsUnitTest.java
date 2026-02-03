package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.commons.logging.util.MdcUtils;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HighLogFilterFunctionsUnitTest {

    @Mock
    private ServerRequest serverRequest;

    @Mock
    private HandlerFunction<ServerResponse> next;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private ServerResponse mockResponse;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private HttpStatus httpStatus;

    private HandlerFilterFunction<ServerResponse, ServerResponse> filter;
    private MockedStatic<MdcUtils> mdcUtilsMock;
    private MockedStatic<MvcUtils> mvcUtilsMock;

    @BeforeEach
    void setUp() {
        filter = HighLogFilterFunctions.addHighLog();
        when(serverRequest.servletRequest()).thenReturn(servletRequest);
        mdcUtilsMock = mockStatic(MdcUtils.class);

        mvcUtilsMock = mockStatic(MvcUtils.class);
        mvcUtilsMock.when(() -> MvcUtils.getApplicationContext(serverRequest)).thenReturn(applicationContext);
    }

    @AfterEach
    void tearDown() {
        mdcUtilsMock.close();
        mvcUtilsMock.close();
    }

    @Test
    void addHighLog_shouldSetRidAndLogRequest() throws Exception {
        when(servletRequest.getServerName()).thenReturn("test.example.com");
        when(servletRequest.getMethod()).thenReturn("GET");
        when(servletRequest.getRequestURI()).thenReturn("/api/test");

        when(next.handle(serverRequest)).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(HttpStatus.OK);

        mdcUtilsMock.when(MdcUtils::generateRid).thenReturn("test-rid-123");
        mdcUtilsMock.when(MdcUtils::getRid).thenReturn("test-rid-123");

        ServerResponse response = filter.filter(serverRequest, next);

        mdcUtilsMock.verify(MdcUtils::generateRid);
        mdcUtilsMock.verify(() -> MdcUtils.putRid("test-rid-123"));

        verify(next).handle(serverRequest);
        verify(mockResponse).statusCode();
        assertEquals(mockResponse, response);
    }

    @Test
    void addHighLog_shouldLogStopWhenRidNotModified() throws Exception {
        when(servletRequest.getServerName()).thenReturn("test.example.com");
        when(servletRequest.getMethod()).thenReturn("POST");
        when(servletRequest.getRequestURI()).thenReturn("/api/users");

        when(next.handle(serverRequest)).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(HttpStatus.CREATED);

        mdcUtilsMock.when(MdcUtils::generateRid).thenReturn("same-rid");
        mdcUtilsMock.when(MdcUtils::getRid).thenReturn("same-rid");

        ServerResponse response = filter.filter(serverRequest, next);

        verify(mockResponse).statusCode();
        assertEquals(mockResponse, response);
    }

    @Test
    void addHighLog_shouldHandleExceptionDuringRidProcessing() throws Exception {
        when(servletRequest.getServerName()).thenReturn("test.example.com");
        when(servletRequest.getMethod()).thenReturn("GET");
        when(servletRequest.getRequestURI()).thenReturn("/api/test");
        when(next.handle(serverRequest)).thenReturn(mockResponse);
        when(mockResponse.statusCode()).thenReturn(HttpStatus.OK);

        mdcUtilsMock.when(MdcUtils::generateRid).thenThrow(new RuntimeException("RID generation failed"));
        mdcUtilsMock.when(MdcUtils::getRid).thenReturn("");

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }
}
