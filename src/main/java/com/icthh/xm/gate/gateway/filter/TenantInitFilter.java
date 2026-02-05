package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.gate.service.TenantMappingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.icthh.xm.gate.config.Constants.DEFAULT_TENANT;
import static com.icthh.xm.gate.config.Constants.FILTER_ORDER_TENANT_INIT;

/**
 * Filter for setting {@link TenantContextHolder}.
 */
@Slf4j
@Component
@Order(FILTER_ORDER_TENANT_INIT)
@RequiredArgsConstructor
public class TenantInitFilter extends OncePerRequestFilter {

    private final TenantMappingService tenantMappingService;
    private final TenantContextHolder tenantContextHolder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String domain = request.getServerName();
        String tenantKeyValue = tenantMappingService.getTenantKey(domain);

        if (!tenantMappingService.isTenantPresent(tenantKeyValue) && !DEFAULT_TENANT.equalsIgnoreCase(tenantKeyValue)) {
            log.error("Tenant {} is not present", tenantKeyValue);
            response(response, "SERVICE-NOT-FOUND");
            return;
        }

        if (!tenantMappingService.isTenantActive(tenantKeyValue) && !DEFAULT_TENANT.equalsIgnoreCase(tenantKeyValue)) {
            log.error("Tenant {} is not active", tenantKeyValue);
            response(response, "SERVICE-SUSPENDED");
            return;
        }

        log.info("Set tenant '{}' to tenant context holder", tenantKeyValue);
        TenantContextUtils.setTenant(tenantContextHolder, tenantKeyValue);
        try {
            filterChain.doFilter(request, response);

        } finally {
            var privilegedTenantContext = tenantContextHolder.getPrivilegedContext();
            String tenantKey = TenantContextUtils.getRequiredTenantKeyValue(privilegedTenantContext);
            log.info("Destroy tenant context holder with value '{}' ", tenantKey);
            privilegedTenantContext.destroyCurrentContext();
        }
    }

    @SneakyThrows
    private static void response(HttpServletResponse response, String code) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\": \"" + code + "\"}");
    }
}
