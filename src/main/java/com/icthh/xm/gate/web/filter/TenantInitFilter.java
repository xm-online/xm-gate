package com.icthh.xm.gate.web.filter;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.gate.service.TenantMappingService;
import com.icthh.xm.gate.utils.ServerRequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static com.icthh.xm.gate.config.Constants.FILTER_ORDER_TENANT_INIT;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

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

        TenantContextUtils.setTenant(tenantContextHolder, tenantKeyValue);
        mdcPutUserAndTenantData(request.getHeaders().getFirst(AUTHORIZATION));
        log.info("Init tenant key: {} to context for {} {}", tenantKeyValue, method, path);

        return chain.filter(exchange)
            .doFinally(onFinally -> {
                tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
                log.info("Destroy current tenant {} context for {} {}", tenantKeyValue, method, path);
                MdcUtils.clear();
            })
            .contextCapture();
    }


    private void mdcPutUserAndTenantData(String requestJwtToken) {
        try {
            String oldRid = MdcUtils.getRid();

            if (StringUtils.isNotEmpty(oldRid)) {
                log.warn("CHECH THIS THREAD " + oldRid);
            }

            String rid = oldRid == null ? MdcUtils.generateRid() : oldRid;
            String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
            String userLogin = ServerRequestUtils.getClientIdFromToken(requestJwtToken);
            MdcUtils.putRid(rid + ":" + userLogin + ":" + tenant);

        } catch (IllegalStateException e) {
            log.error(e.getMessage());
            throw e;
        }
    }

}
