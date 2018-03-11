package com.icthh.xm.gate.gateway;

import static com.icthh.xm.gate.config.Constants.FILTER_ORDER_TENANT_INIT;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.gate.service.TenantMappingService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Filter for setting {@link com.icthh.xm.commons.tenant.TenantContextHolder}.
 */
@Slf4j
@AllArgsConstructor
@Order(FILTER_ORDER_TENANT_INIT)
@Component
public class TenantInitFilter implements Filter {

    private final TenantMappingService tenantMappingService;
    private final TenantContextHolder tenantContextHolder;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        String domain = servletRequest.getServerName();
        String tenantKeyValue = tenantMappingService.getTenantKey(domain);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKeyValue);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        }
    }

    @Override
    public void destroy() {

    }

}
