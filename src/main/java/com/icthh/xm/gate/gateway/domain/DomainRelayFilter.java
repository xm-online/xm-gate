package com.icthh.xm.gate.gateway.domain;

import com.icthh.xm.gate.service.TenantMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URL;

import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static com.icthh.xm.gate.config.Constants.HEADER_WEBAPP_URL;

/**
 * Gateway filter to proxy subdomain to UAA.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DomainRelayFilter implements GlobalFilter {

    private final TenantMappingService tenantMappingService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = HttpHeaders.writableHttpHeaders(exchange.getRequest().getHeaders());

        String domain = exchange.getRequest().getURI().getHost();

        headers.add(HEADER_TENANT, tenantMappingService.getTenantKey(domain));
        headers.add(HEADER_WEBAPP_URL, getRefererUri(exchange.getRequest()));

        return chain.filter(exchange);
    }

    private static String getRefererUri(ServerHttpRequest request) {
        String referer = request.getHeaders().getFirst(HttpHeaders.REFERER);
        if (StringUtils.isNotBlank(referer)) {
            try {
                URL url = new URL(referer);
                URI uri = new URI(url.getProtocol(), url.getAuthority(), null, null, null);
                return uri.toString();
            } catch (Exception e) {
                log.debug("Error while converting referer header to URI, referer={}", referer, e);
            }
        }
        return null;
    }
}
