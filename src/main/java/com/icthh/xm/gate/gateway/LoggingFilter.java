package com.icthh.xm.gate.gateway;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Component;

/**
 * Filter for logging all HTTP requests and set MDC context RID variable.
 */
@Slf4j
@AllArgsConstructor
@Component
public class LoggingFilter implements Filter {

    private static final String MANAGEMENT_HEALTH_URI = "/management/health";
    private final TenantContextHolder tenantContextHolder;

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {

        StopWatch stopWatch = StopWatch.createStarted();

        String domain = request.getServerName();
        String remoteAddr = request.getRemoteAddr();
        Long contentLength = request.getContentLengthLong();

        String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);

        String method = null;
        String userLogin = null;
        String requestUri = null;

        try {

            if (request instanceof HttpServletRequest) {
                HttpServletRequest req = (HttpServletRequest) request;
                method = req.getMethod();
                userLogin = req.getRemoteUser();
                requestUri = req.getRequestURI();

                if (MANAGEMENT_HEALTH_URI.equals(requestUri)) {
                    chain.doFilter(request, response);
                    return;
                }
            }
            String oldRid = MdcUtils.getRid();
            String rid = oldRid == null ? MdcUtils.generateRid() : oldRid;

            MdcUtils.putRid(rid + ":" + userLogin + ":" + tenant);

            log.info("START {}/{} --> {} {}, contentLength = {} ", remoteAddr, domain, method, requestUri,
                contentLength);

            chain.doFilter(request, response);

            Integer status = null;

            if (response instanceof HttpServletResponse) {
                HttpServletResponse res = (HttpServletResponse) response;
                status = res.getStatus();
            }

            log.info("STOP  {}/{} --> {} {}, status = {}, time = {} ms", remoteAddr, domain, method, requestUri,
                status, stopWatch.getTime());

        } catch (Exception e) {
            log.error("STOP  {}/{} --> {} {}, error = {}, time = {} ms", remoteAddr, domain, method, requestUri,
                LogObjectPrinter.printException(e), stopWatch.getTime());
            throw e;
        } finally {
            MdcUtils.clear();
        }

    }
}
