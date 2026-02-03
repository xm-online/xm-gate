package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.commons.logging.util.LogObjectPrinter;
import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Function for logging all HTTP requests and set MDC context RID variable.
 */
@Slf4j
public class LoggingFilterFunctions {

    private static final String MANAGEMENT_HEALTH_URI = "/management/health";

    public static HandlerFilterFunction<ServerResponse, ServerResponse> addLogging() {
        return (request, next) -> {
            HttpServletRequest servletRequest = request.servletRequest();

            if (MANAGEMENT_HEALTH_URI.equals(servletRequest.getRequestURI())) {
                return next.handle(request);
            }

            TenantContextHolder tenantContextHolder = MvcUtils.getApplicationContext(request)
                .getBean(TenantContextHolder.class);

            StopWatch stopWatch = StopWatch.createStarted();

            String domain = servletRequest.getServerName();
            String remoteAddr = servletRequest.getRemoteAddr();
            Long contentLength = servletRequest.getContentLengthLong();

            String tenant = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);

            String method = servletRequest.getMethod();
            String userLogin = servletRequest.getRemoteUser();
            String requestUri = servletRequest.getRequestURI();

            try {
                String oldRid = MdcUtils.getRid();
                String rid = oldRid == null ? MdcUtils.generateRid() : oldRid;

                MdcUtils.putRid(rid + ":" + userLogin + ":" + tenant);

                log.info("START {}/{} --> {} {}, contentLength = {} ", remoteAddr, domain, method, requestUri,
                    contentLength);

                ServerResponse response = next.handle(request);

                log.info("STOP  {}/{} --> {} {}, status = {}, time = {} ms", remoteAddr, domain, method, requestUri,
                    response.statusCode().value(), stopWatch.getTime());

                return response;

            } catch (Exception e) {
                log.error("STOP  {}/{} --> {} {}, error = {}, time = {} ms", remoteAddr, domain, method, requestUri,
                    LogObjectPrinter.printException(e), stopWatch.getTime());
                throw e;
            } finally {
                MdcUtils.clear();
            }
        };
    }

    public static class FilterSupplier extends SimpleFilterSupplier {
        public FilterSupplier() {
            super(LoggingFilterFunctions.class);
        }
    }
}
