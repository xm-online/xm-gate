package com.icthh.xm.gate.gateway;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
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

    private final TenantContextHolder tenantContextHolder;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

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
                HttpServletRequest req = HttpServletRequest.class.cast(request);
                method = req.getMethod();
                userLogin = req.getRemoteUser();
                requestUri = req.getRequestURI();
            }

            MdcUtils.putRid(MdcUtils.generateRid() + ":" + userLogin + ":" + tenant);

            log.info("START {}/{} --> {} {}, contentLength = {} ", remoteAddr, domain, method, requestUri,
                     contentLength);

            chain.doFilter(request, response);

            Integer status = null;

            if (response instanceof HttpServletResponse) {
                HttpServletResponse res = HttpServletResponse.class.cast(response);
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

    @Override
    public void destroy() {

    }
}
