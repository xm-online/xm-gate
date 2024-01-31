package com.icthh.xm.gate.gateway;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
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

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String requestUri = request.getURI().getPath();

        if (MANAGEMENT_HEALTH_URI.equals(requestUri)) {
            return chain.filter(exchange);
        }
        mdcPutUserAndTenantData(request.getHeaders().getFirst(AUTHORIZATION));

        StopWatch stopWatch = StopWatch.createStarted();

        String domain = request.getURI().getHost();
        String remoteAddr = Objects.requireNonNull(request.getRemoteAddress()).getAddress().getHostAddress();
        String method = request.getMethod().name();
        Long contentLength = request.getHeaders().getContentLength();

        log.info("START {}/{} --> {} {}, contentLength = {} ", remoteAddr, domain, method, requestUri, contentLength);

        return chain.filter(exchange)
            .doOnSuccess(signal -> {
                ServerHttpResponse response = exchange.getResponse();
                Integer status = Objects.requireNonNull(response.getStatusCode()).value();

                log.info("STOP  {}/{} --> {} {}, status = {}, time = {} ms", remoteAddr, domain, method, requestUri,
                    status, stopWatch.getTime());
            })
            .doOnError(signal -> {
                log.error("STOP  {}/{} --> {} {}, error = {}, time = {} ms", remoteAddr, domain, method, requestUri,
                    LogObjectPrinter.printException(signal.getCause()), stopWatch.getTime());
                throw new RuntimeException(signal);
            })
            .doFinally(onFinally -> MdcUtils.clear())
            .contextCapture();
    }

    private void mdcPutUserAndTenantData(String requestJwtToken) {
        try {
            String oldRid = MdcUtils.getRid();
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
