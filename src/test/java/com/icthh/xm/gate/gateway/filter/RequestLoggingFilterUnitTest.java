package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterUnitTest {

    private static final String TENANT = "TEST";
    private static final String HEALTH_URI = "/management/health";
    private static final String EXEC_TIME_MDC_KEY = "execTime";

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private FilterChain filterChain;

    private RequestLoggingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockedStatic<TenantContextUtils> tenantContextUtilsMock;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter(tenantContextHolder);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setServerName("test.example.com");
        request.setMethod("GET");
        request.setRemoteAddr("127.0.0.1");
        request.setRemoteUser("test-client");

        tenantContextUtilsMock = Mockito.mockStatic(TenantContextUtils.class);
        tenantContextUtilsMock.when(() -> TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder))
            .thenReturn(TENANT);
    }

    @AfterEach
    void tearDown() {
        tenantContextUtilsMock.close();
        MDC.clear();
    }

    @Test
    void healthCheck_bypassesLoggingAndTenantLookup() throws Exception {
        request.setRequestURI(HEALTH_URI);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(tenantContextHolder);
        assertNull(MDC.get(EXEC_TIME_MDC_KEY));
    }

    @Test
    void success_setsCompositeRidDuringRequest_clearsRidAndExecTimeAfter() throws Exception {
        request.setRequestURI("/api/some-resource");
        response.setStatus(200);

        String[] ridDuringRequest = new String[1];
        doAnswer(invocation -> {
            ridDuringRequest[0] = MDC.get("rid");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        String[] parts = ridDuringRequest[0].split(":");
        assertEquals(3, parts.length);
        assertEquals("test-client", parts[1]);
        assertEquals(TENANT, parts[2]);
        assertNull(MDC.get("rid"));
        assertNull(MDC.get(EXEC_TIME_MDC_KEY));
    }

    @Test
    void success_reusesExistingRid_ifAlreadyPresent() throws Exception {
        MDC.put("rid", "existingRid");
        request.setRequestURI("/api/some-resource");

        String[] ridDuringRequest = new String[1];
        doAnswer(invocation -> {
            ridDuringRequest[0] = MDC.get("rid");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("existingRid:test-client:" + TENANT, ridDuringRequest[0]);
    }

    @Test
    void exception_isRethrown_ridAndExecTimeStillCleared() throws Exception {
        request.setRequestURI("/api/some-resource");
        doThrow(new ServletException("downstream failure")).when(filterChain).doFilter(request, response);

        assertThrows(ServletException.class,
            () -> filter.doFilterInternal(request, response, filterChain));

        assertNull(MDC.get("rid"));
        assertNull(MDC.get(EXEC_TIME_MDC_KEY));
    }

    @Test
    void exception_ioException_ridAndExecTimeStillCleared() throws Exception {
        request.setRequestURI("/api/some-resource");
        doThrow(new IOException("io failure")).when(filterChain).doFilter(request, response);

        assertThrows(IOException.class,
            () -> filter.doFilterInternal(request, response, filterChain));

        assertNull(MDC.get("rid"));
        assertNull(MDC.get(EXEC_TIME_MDC_KEY));
    }
}
