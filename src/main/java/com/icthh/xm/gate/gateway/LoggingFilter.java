package com.icthh.xm.gate.gateway;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.gate.service.TenantMappingService;
import com.icthh.xm.gate.utils.ServerRequestUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static org.apache.http.HttpHeaders.AUTHORIZATION;

/**
 * Filter for logging all HTTP requests and set MDC context RID variable.
 */
@Slf4j
@AllArgsConstructor
@Component
public class LoggingFilter implements WebFilter {

    private static final String MANAGEMENT_HEALTH_URI = "/management/health";
    private final TenantContextHolder tenantContextHolder;
    private final TenantMappingService tenantMappingService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        StopWatch stopWatch = StopWatch.createStarted();

        String domain = request.getURI().getHost();
        String remoteAddr = Objects.requireNonNull(request.getRemoteAddress()).getAddress().getHostAddress();
        Long contentLength = request.getHeaders().getContentLength();

        TenantContextUtils.setTenant(tenantContextHolder, tenantMappingService.getTenantKey(domain)); // todo: remove

        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);

        String method = null;
        String userLogin = null;
        String requestUri = null;

        try {
            method = request.getMethod().name();
            userLogin = ServerRequestUtils.getClientIdFromToken(request.getHeaders().getFirst(AUTHORIZATION));
            requestUri = request.getURI().getPath();

            if (MANAGEMENT_HEALTH_URI.equals(requestUri)) {
                return chain.filter(exchange);
            }
            String oldRid = MdcUtils.getRid();
            String rid = oldRid == null ? MdcUtils.generateRid() : oldRid;

            MdcUtils.putRid(rid + ":" + userLogin + ":" + tenant);

            log.info("START {}/{} --> {} {}, contentLength = {} ", remoteAddr, domain, method, requestUri,
                contentLength);

            return chain.filter(exchange).doOnEach(signal -> {
                ServerHttpResponse response = exchange.getResponse();
                Integer status = Objects.requireNonNull(response.getStatusCode()).value();

                log.info("STOP  {}/{} --> {} {}, status = {}, time = {} ms", remoteAddr, domain,
                    request.getMethod().name(), request.getURI().getPath(), status, stopWatch.getTime());
            });

        } catch (Exception e) {
            log.error("STOP  {}/{} --> {} {}, error = {}, time = {} ms", remoteAddr, domain, method, requestUri,
                LogObjectPrinter.printException(e), stopWatch.getTime());
            throw e;
        } finally {
            MdcUtils.clear();
        }
    }
}
