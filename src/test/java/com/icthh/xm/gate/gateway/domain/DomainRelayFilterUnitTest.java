package com.icthh.xm.gate.gateway.domain;

import static com.icthh.xm.gate.config.Constants.HEADER_DOMAIN;
import static com.icthh.xm.gate.config.Constants.HEADER_PORT;
import static com.icthh.xm.gate.config.Constants.HEADER_SCHEME;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.icthh.xm.gate.service.TenantMappingService;
import com.netflix.zuul.ExecutionStatus;
import com.netflix.zuul.ZuulFilterResult;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.monitoring.MonitoringHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;

/**
 * Tests DomainRelayFilter class.
 */
public class DomainRelayFilterUnitTest {

    private static final String SERVER_PROTOCOL = "FIELDS.OF.GOLD";
    private static final String SERVER_NAME = "xm.local";
    private static final String TENANT = "XM";
    private static final int SERVER_PORT = 777;

    private DomainRelayFilter filter;

    @Mock
    private TenantMappingService tenantMappingService;

    @Mock
    private HttpServletRequest request;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        MonitoringHelper.initMocks();
        filter = new DomainRelayFilter(tenantMappingService);
        when(tenantMappingService.getTenantKey(any())).thenReturn(TENANT);
    }

    @Test
    public void shouldFilter() {
        RequestContext.testSetCurrentContext(new RequestContext());
        when(request.getScheme()).thenReturn(SERVER_PROTOCOL);
        when(request.getServerName()).thenReturn(SERVER_NAME);
        when(request.getServerPort()).thenReturn(SERVER_PORT);
        RequestContext.getCurrentContext().setRequest(request);

        ZuulFilterResult result = filter.runFilter();

        Assert.assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertTrue(filter.shouldFilter());
        Assert.assertEquals("pre", filter.filterType());
        Assert.assertEquals(0, filter.filterOrder());
        Assert.assertEquals(SERVER_PROTOCOL, RequestContext.getCurrentContext().getZuulRequestHeaders().get(
            HEADER_SCHEME));
        Assert.assertEquals(SERVER_NAME, RequestContext.getCurrentContext().getZuulRequestHeaders().get(HEADER_DOMAIN));
        Assert.assertEquals(String.valueOf(SERVER_PORT), RequestContext.getCurrentContext().getZuulRequestHeaders().get(HEADER_PORT));
        Assert.assertEquals(TENANT, RequestContext.getCurrentContext().getZuulRequestHeaders().get(HEADER_TENANT));
    }

    @Test
    public void shouldFilterThrow() {
        RequestContext.testSetCurrentContext(new RequestContext());
        when(request.getScheme()).thenReturn(SERVER_PROTOCOL);
        when(request.getServerName()).thenReturn("NOMAPPING");
        when(request.getServerPort()).thenReturn(SERVER_PORT);
        RequestContext.getCurrentContext().setRequest(request);

        ZuulFilterResult result = filter.runFilter();
        Assert.assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        Assert.assertEquals(TENANT, RequestContext.getCurrentContext().getZuulRequestHeaders().get(HEADER_TENANT));
    }

}
