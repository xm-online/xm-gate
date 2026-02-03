package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.commons.tenant.TenantContext;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.gate.security.oauth2.idp.IdpConfigRepository;
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
class IdpStatefulModeFilterFunctionsUnitTest {

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
    private IdpConfigRepository idpConfigRepository;

    @Mock
    private XmAuthenticationContextHolder authenticationContextHolder;

    @Mock
    private XmAuthenticationContext xmAuthenticationContext;

    @Mock
    private IdpPublicConfig.IdpConfigContainer.Features features;

    @Mock
    private ServerResponse mockResponse;

    private HandlerFilterFunction<ServerResponse, ServerResponse> filter;
    private MockedStatic<MvcUtils> mvcUtilsMock;

    @BeforeEach
    void setUp() {
        filter = IdpStatefulModeFilterFunctions.idpStatefulMode();
        when(serverRequest.servletRequest()).thenReturn(servletRequest);

        mvcUtilsMock = mockStatic(MvcUtils.class);
        mvcUtilsMock.when(() -> MvcUtils.getApplicationContext(serverRequest)).thenReturn(applicationContext);
    }

    @AfterEach
    void tearDown() {
        mvcUtilsMock.close();
    }

    @Test
    void idpStatefulMode_shouldSkipFilter_whenPathDoesNotContainApi() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/public/resource");
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void idpStatefulMode_shouldSkipFilter_whenPathIsStatic() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/static/js/app.js");
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void idpStatefulMode_shouldProceed_whenNoAuthentication() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/api/test");

        when(applicationContext.getBean(TenantContextHolder.class)).thenReturn(tenantContextHolder);
        when(applicationContext.getBean(IdpConfigRepository.class)).thenReturn(idpConfigRepository);
        when(applicationContext.getBean(XmAuthenticationContextHolder.class)).thenReturn(authenticationContextHolder);

        when(authenticationContextHolder.getContext()).thenReturn(xmAuthenticationContext);
        when(xmAuthenticationContext.hasAuthentication()).thenReturn(false);
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        verify(idpConfigRepository, never()).getTenantFeatures(org.mockito.ArgumentMatchers.anyString());
        assertEquals(mockResponse, response);
    }

    @Test
    void idpStatefulMode_shouldProceed_whenStatelessMode() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/api/test");

        when(applicationContext.getBean(TenantContextHolder.class)).thenReturn(tenantContextHolder);
        when(applicationContext.getBean(IdpConfigRepository.class)).thenReturn(idpConfigRepository);
        when(applicationContext.getBean(XmAuthenticationContextHolder.class)).thenReturn(authenticationContextHolder);

        when(authenticationContextHolder.getContext()).thenReturn(xmAuthenticationContext);
        when(xmAuthenticationContext.hasAuthentication()).thenReturn(true);

        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));

        when(idpConfigRepository.getTenantFeatures("TEST")).thenReturn(features);
        when(features.isStateful()).thenReturn(false);
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void idpStatefulMode_shouldThrowException_whenStatefulMode() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/api/test");

        when(applicationContext.getBean(TenantContextHolder.class)).thenReturn(tenantContextHolder);
        when(applicationContext.getBean(IdpConfigRepository.class)).thenReturn(idpConfigRepository);
        when(applicationContext.getBean(XmAuthenticationContextHolder.class)).thenReturn(authenticationContextHolder);

        when(authenticationContextHolder.getContext()).thenReturn(xmAuthenticationContext);
        when(xmAuthenticationContext.hasAuthentication()).thenReturn(true);

        when(tenantContextHolder.getContext()).thenReturn(tenantContext);
        when(tenantContext.getTenantKey()).thenReturn(Optional.of(TenantKey.valueOf("TEST")));

        when(idpConfigRepository.getTenantFeatures("TEST")).thenReturn(features);
        when(features.isStateful()).thenReturn(true);

        assertThrows(UnsupportedOperationException.class, () -> filter.filter(serverRequest, next));
    }
}
