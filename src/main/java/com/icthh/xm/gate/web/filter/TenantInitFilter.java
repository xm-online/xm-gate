package com.icthh.xm.gate.web.filter;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.gate.service.TenantMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        String tenantKeyValue = tenantMappingService.getTenantKey(domain);

        log.info("Init tenant key: {} to context for {} {}", tenantKeyValue, method, path);
        TenantContextUtils.setTenant(tenantContextHolder, tenantKeyValue);

        return chain.filter(exchange)
            .doFinally(onFinally -> {
                tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
                log.info("Destroy current tenant {} context for {} {}", tenantKeyValue, method, path);
            })
            .contextCapture();
    }
}
