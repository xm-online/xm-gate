package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.gate.service.TenantMappingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class
TenantInitFilterUnitTest {

    @Mock
    private TenantMappingService tenantMappingService;

    @Mock
    private TenantContextHolder tenantContextHolder;

    @Mock
    private PrivilegedTenantContext privilegedTenantContext;

    @Mock
    private FilterChain filterChain;

    private TenantInitFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new TenantInitFilter(tenantMappingService, tenantContextHolder);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void tenantInit_shouldReturnBadRequest_whenTenantNotPresent() throws Exception {
        request.setServerName("unknown.example.com");
        when(tenantMappingService.getTenantKey("unknown.example.com")).thenReturn("UNKNOWN");
        when(tenantMappingService.isTenantPresent("UNKNOWN")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(400, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals("{\"error\": \"SERVICE-NOT-FOUND\"}", response.getContentAsString());
        verifyNoInteractions(filterChain);
    }

    @Test
    void tenantInit_shouldReturnBadRequest_whenTenantNotActive() throws Exception {
        request.setServerName("inactive.example.com");
        when(tenantMappingService.getTenantKey("inactive.example.com")).thenReturn("INACTIVE");
        when(tenantMappingService.isTenantPresent("INACTIVE")).thenReturn(true);
        when(tenantMappingService.isTenantActive("INACTIVE")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(400, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEquals("{\"error\": \"SERVICE-SUSPENDED\"}", response.getContentAsString());
        verifyNoInteractions(filterChain);
    }

    @Test
    void tenantInit_shouldSetTenantAndProceed_whenTenantValid() throws Exception {
        request.setServerName("test.example.com");
        when(tenantMappingService.getTenantKey("test.example.com")).thenReturn("TEST");
        when(tenantMappingService.isTenantPresent("TEST")).thenReturn(true);
        when(tenantMappingService.isTenantActive("TEST")).thenReturn(true);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
        when(privilegedTenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(privilegedTenantContext).destroyCurrentContext();
    }

    @Test
    void tenantInit_shouldAllowDefaultTenant_evenWhenNotPresent() throws Exception {
        request.setServerName("xm.example.com");
        when(tenantMappingService.getTenantKey("xm.example.com")).thenReturn("XM");
        when(tenantMappingService.isTenantPresent("XM")).thenReturn(false);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
        when(privilegedTenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("XM")));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(privilegedTenantContext).destroyCurrentContext();
    }

    @Test
    void tenantInit_shouldAllowDefaultTenant_evenWhenNotActive() throws Exception {
        request.setServerName("xm.example.com");
        when(tenantMappingService.getTenantKey("xm.example.com")).thenReturn("xm");
        when(tenantMappingService.isTenantPresent("xm")).thenReturn(true);
        when(tenantMappingService.isTenantActive("xm")).thenReturn(false);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
        when(privilegedTenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("xm")));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(privilegedTenantContext).destroyCurrentContext();
    }

    @Test
    void tenantInit_shouldDestroyContext_evenOnException() throws Exception {
        request.setServerName("test.example.com");
        when(tenantMappingService.getTenantKey("test.example.com")).thenReturn("TEST");
        when(tenantMappingService.isTenantPresent("TEST")).thenReturn(true);
        when(tenantMappingService.isTenantActive("TEST")).thenReturn(true);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
        when(privilegedTenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));
        doThrow(new ServletException("Test error")).when(filterChain).doFilter(request, response);

        assertThrows(ServletException.class,
            () -> filter.doFilterInternal(request, response, filterChain));

        verify(privilegedTenantContext).destroyCurrentContext();
    }

    @Test
    void tenantInit_shouldDestroyContext_evenOnIOException() throws Exception {
        request.setServerName("test.example.com");
        when(tenantMappingService.getTenantKey("test.example.com")).thenReturn("TEST");
        when(tenantMappingService.isTenantPresent("TEST")).thenReturn(true);
        when(tenantMappingService.isTenantActive("TEST")).thenReturn(true);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
        when(privilegedTenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));
        doThrow(new IOException("IO error")).when(filterChain).doFilter(request, response);

        assertThrows(IOException.class,
            () -> filter.doFilterInternal(request, response, filterChain));

        verify(privilegedTenantContext).destroyCurrentContext();
    }
}
