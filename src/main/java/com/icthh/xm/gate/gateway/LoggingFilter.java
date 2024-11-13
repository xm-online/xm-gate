package com.icthh.xm.gate.gateway;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import com.icthh.xm.gate.utils.MdcMonitoringUtils;
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

/**
 * Filter for logging all HTTP requests and set MDC context RID variable.
 */
@Slf4j
@AllArgsConstructor
@Component
public class LoggingFilter implements WebFilter {

    private static final String MANAGEMENT_HEALTH_URI = "/management/health";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String requestUri = request.getURI().getPath();

        if (MANAGEMENT_HEALTH_URI.equals(requestUri)) {
            return chain.filter(exchange);
        }

        StopWatch stopWatch = StopWatch.createStarted();

        String domain = request.getURI().getHost();
        String remoteAddr = Objects.requireNonNull(request.getRemoteAddress()).getAddress().getHostAddress();
        String method = request.getMethod().name();
        Long contentLength = request.getHeaders().getContentLength();
        String clientId = ServerRequestUtils.getClientIdFromRequest(request);

        log.info("START {}/{} --> {} {}, contentLength = {} ", remoteAddr, domain, method, requestUri, contentLength);

        return chain.filter(exchange)
            .doOnSuccess(signal -> {
                Integer status = getHttpStatusCode(exchange);
                long requestDuration = stopWatch.getTime();

                MdcMonitoringUtils.setMonitoringKeys(method, status, requestDuration, clientId, requestUri);

                log.info("STOP  {}/{} --> {} {}, status = {}, time = {} ms", remoteAddr, domain, method, requestUri,
                    status, requestDuration);
                MdcMonitoringUtils.clearMonitoringKeys();
            })
            .doOnError(signal -> {
                MdcMonitoringUtils.setMonitoringKeys(method, getHttpStatusCode(exchange), stopWatch.getTime(), clientId, requestUri);
                String exception = (signal == null || signal.getCause() == null) ? "unknown" : LogObjectPrinter.printException(signal.getCause());
                log.error("STOP  {}/{} --> {} {}, error = {}, time = {} ms", remoteAddr, domain, method, requestUri, exception, stopWatch.getTime());
                MdcMonitoringUtils.clearMonitoringKeys();
                throw new RuntimeException(signal);
            })
            .contextCapture();
    }

    private Integer getHttpStatusCode(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        return Objects.requireNonNull(response.getStatusCode()).value();
    }

}
