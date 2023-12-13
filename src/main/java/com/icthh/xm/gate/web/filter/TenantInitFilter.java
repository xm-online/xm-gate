package com.icthh.xm.gate.web.filter;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.gate.service.TenantMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static com.icthh.xm.gate.config.Constants.FILTER_ORDER_TENANT_INIT;

/**
 * Filter for setting {@link TenantContextHolder}.
 */
@Component
@Order(FILTER_ORDER_TENANT_INIT)
@RequiredArgsConstructor
public class TenantInitFilter implements WebFilter {

    private final TenantMappingService tenantMappingService;

    private final TenantContextHolder tenantContextHolder;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String domain = request.getURI().getHost();
        String tenantKeyValue = tenantMappingService.getTenantKey(domain);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKeyValue);
        try {
            return chain.filter(exchange);
        } finally {
            tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        }
    }
}
