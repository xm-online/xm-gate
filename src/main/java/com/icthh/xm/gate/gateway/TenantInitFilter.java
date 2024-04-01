package com.icthh.xm.gate.gateway;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.gate.config.ApplicationProperties;
import com.icthh.xm.gate.service.TenantMappingService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static com.icthh.xm.gate.config.Constants.DEFAULT_TENANT;
import static com.icthh.xm.gate.config.Constants.FILTER_ORDER_TENANT_INIT;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.springframework.http.HttpHeaders.LOCATION;

/**
 * Filter for setting {@link TenantContextHolder}.
 */
@Slf4j
@AllArgsConstructor
@Order(FILTER_ORDER_TENANT_INIT)
@Component
public class TenantInitFilter implements Filter {

    private final TenantMappingService tenantMappingService;
    private final TenantContextHolder tenantContextHolder;
    private final ApplicationProperties applicationProperties;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        String domain = servletRequest.getServerName();
        String tenantKeyValue = tenantMappingService.getTenantKey(domain);

        if (!tenantMappingService.isTenantPresent(tenantKeyValue) && !DEFAULT_TENANT.equalsIgnoreCase(tenantKeyValue)) {
            log.error("Tenant {} is not present", tenantKeyValue);
            response(servletResponse, "SERVICE-NOT-FOUND");
            return;
        }

        if (!tenantMappingService.isTenantActive(tenantKeyValue) && !DEFAULT_TENANT.equalsIgnoreCase(tenantKeyValue)) {
            log.error("Tenant {} is not active", tenantKeyValue);
            response(servletResponse, "SERVICE-SUSPENDED");
            return;
        }

        TenantContextUtils.setTenant(tenantContextHolder, tenantKeyValue);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        }
    }

    @SneakyThrows
    private static void response(ServletResponse servletResponse, String code) {
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        httpResponse.setContentType("application/json");
        httpResponse.setCharacterEncoding("UTF-8");

        String jsonContent = "{\"error\": \"" + code + "\"}";

        PrintWriter out = httpResponse.getWriter();
        out.print(jsonContent);
        out.flush();
    }

    @Override
    public void destroy() {

    }

}
