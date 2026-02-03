package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.commons.tenant.PrivilegedTenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.gate.service.TenantMappingService;
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
import org.springframework.web.servlet.function.EntityResponse;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantInitFilterFunctionsUnitTest {

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
    private TenantContextHolder tenantContextHolder;

    @Mock
    private PrivilegedTenantContext privilegedTenantContext;

    @Mock
    private ServerResponse mockResponse;

    private HandlerFilterFunction<ServerResponse, ServerResponse> filter;
    private MockedStatic<MvcUtils> mvcUtilsMock;

    @BeforeEach
    void setUp() {
        filter = TenantInitFilterFunctions.tenantInit();
        when(serverRequest.servletRequest()).thenReturn(servletRequest);

        when(applicationContext.getBean(TenantMappingService.class)).thenReturn(tenantMappingService);
        when(applicationContext.getBean(TenantContextHolder.class)).thenReturn(tenantContextHolder);

        mvcUtilsMock = mockStatic(MvcUtils.class);
        mvcUtilsMock.when(() -> MvcUtils.getApplicationContext(serverRequest)).thenReturn(applicationContext);
    }

    @AfterEach
    void tearDown() {
        mvcUtilsMock.close();
    }

    @Test
    void tenantInit_shouldReturnBadRequest_whenTenantNotPresent() throws Exception {
        when(servletRequest.getServerName()).thenReturn("unknown.example.com");
        when(tenantMappingService.getTenantKey("unknown.example.com")).thenReturn("UNKNOWN");
        when(tenantMappingService.isTenantPresent("UNKNOWN")).thenReturn(false);

        ServerResponse response = filter.filter(serverRequest, next);

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());
        assertEquals("{\"error\": \"SERVICE-NOT-FOUND\"}", ((EntityResponse<String>) response).entity());
    }

    @Test
    void tenantInit_shouldReturnBadRequest_whenTenantNotActive() throws Exception {
        when(servletRequest.getServerName()).thenReturn("inactive.example.com");
        when(tenantMappingService.getTenantKey("inactive.example.com")).thenReturn("INACTIVE");
        when(tenantMappingService.isTenantPresent("INACTIVE")).thenReturn(true);
        when(tenantMappingService.isTenantActive("INACTIVE")).thenReturn(false);

        ServerResponse response = filter.filter(serverRequest, next);

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode());
        assertEquals("{\"error\": \"SERVICE-SUSPENDED\"}", ((EntityResponse<String>) response).entity());
    }

    @Test
    void tenantInit_shouldSetTenantAndProceed_whenTenantValid() throws Exception {
        when(servletRequest.getServerName()).thenReturn("test.example.com");
        when(tenantMappingService.getTenantKey("test.example.com")).thenReturn("TEST");
        when(tenantMappingService.isTenantPresent("TEST")).thenReturn(true);
        when(tenantMappingService.isTenantActive("TEST")).thenReturn(true);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        verify(privilegedTenantContext).destroyCurrentContext();
        assertEquals(mockResponse, response);
    }

    @Test
    void tenantInit_shouldAllowDefaultTenant_evenWhenNotPresent() throws Exception {
        when(servletRequest.getServerName()).thenReturn("xm.example.com");
        when(tenantMappingService.getTenantKey("xm.example.com")).thenReturn("XM");
        when(tenantMappingService.isTenantPresent("XM")).thenReturn(false);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void tenantInit_shouldAllowDefaultTenant_evenWhenNotActive() throws Exception {
        when(servletRequest.getServerName()).thenReturn("xm.example.com");
        when(tenantMappingService.getTenantKey("xm.example.com")).thenReturn("xm");
        when(tenantMappingService.isTenantPresent("xm")).thenReturn(true);
        when(tenantMappingService.isTenantActive("xm")).thenReturn(false);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void tenantInit_shouldDestroyContext_evenOnException() throws Exception {
        when(servletRequest.getServerName()).thenReturn("test.example.com");
        when(tenantMappingService.getTenantKey("test.example.com")).thenReturn("TEST");
        when(tenantMappingService.isTenantPresent("TEST")).thenReturn(true);
        when(tenantMappingService.isTenantActive("TEST")).thenReturn(true);
        when(tenantContextHolder.getPrivilegedContext()).thenReturn(privilegedTenantContext);
        when(next.handle(serverRequest)).thenThrow(new RuntimeException("Test error"));

        assertThrows(RuntimeException.class,
            () -> filter.filter(serverRequest, next),
            "Test error");

        verify(privilegedTenantContext).destroyCurrentContext();
    }
}
