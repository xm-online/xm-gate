package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.icthh.xm.gate.config.Constants.FILTER_ORDER_TENANT_INIT;

/**
 * Replaces the former route-level function filters AddHighLog, AddLogging with a real Servlet Filter,
 * so every request is logged
 */
@Slf4j
@Component
@Order(FILTER_ORDER_TENANT_INIT + 1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String MANAGEMENT_HEALTH_URI = "/management/health";
    static final String ACTIVE_REQUESTS_METRIC = "gate.requests.active";

    private final TenantContextHolder tenantContextHolder;
    // strong reference required: the registry holds the gauge object weakly
    private final AtomicInteger activeRequests;

    public RequestLoggingFilter(TenantContextHolder tenantContextHolder, MeterRegistry meterRegistry) {
        this.tenantContextHolder = tenantContextHolder;
        this.activeRequests = meterRegistry.gauge(ACTIVE_REQUESTS_METRIC, new AtomicInteger());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {

        if (MANAGEMENT_HEALTH_URI.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String domain = request.getServerName();
        String remoteAddr = request.getRemoteAddr();
        long contentLength = request.getContentLengthLong();
        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        String method = request.getMethod();
        String userLogin = request.getRemoteUser();
        String requestUri = request.getRequestURI();

        int activeAtStart = activeRequests.incrementAndGet();
        try {
            String oldRid = MdcUtils.getRid();
            String rid = oldRid == null ? MdcUtils.generateRid() : oldRid;
            MdcUtils.putRid(rid + ":" + userLogin + ":" + tenant);
            MdcUtils.putTenant(tenant);

            log.info("START {}/{} --> {} {}, contentLength = {}, active = {}",
                remoteAddr, domain, method, requestUri, contentLength, activeAtStart);

            chain.doFilter(request, response);

            log.info("STOP  {}/{} --> {} {}, status = {}, active = {}, time = {} ms",
                remoteAddr, domain, method, requestUri, response.getStatus(), activeRequests.get(), MdcUtils.putExecTimeMs());
        } catch (Exception e) {
            log.error("STOP  {}/{} --> {} {}, error = {}, active = {}, time = {} ms",
                remoteAddr, domain, method, requestUri, LogObjectPrinter.printException(e), activeRequests.get(), MdcUtils.putExecTimeMs());
            throw e;
        } finally {
            activeRequests.decrementAndGet();
            MdcUtils.clear();
        }
    }
}
