package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.commons.logging.util.MdcUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Function for logging all HTTP requests with highest precedence
 */
@Slf4j
public class HighLogFilterFunctions {

    public static HandlerFilterFunction<ServerResponse, ServerResponse> addHighLog() {
        return (request, next) -> {
            HttpServletRequest servletRequest = request.servletRequest();

            String domain = servletRequest.getServerName();
            String method = servletRequest.getMethod();
            String requestUri = servletRequest.getRequestURI();

            String rid = "";

            try {
                rid = MdcUtils.generateRid();
                MdcUtils.putRid(rid);

                log.info("PRE FILTER: START: {} --> {} {}", domain, method, requestUri);
            } catch (Exception ex) {
                log.warn("Error during RiD processing", ex);
            }

            ServerResponse response = next.handle(request);

            // if Rid was not modified by standard LoggingFilter we need to log the http status
            if (rid.equals(MdcUtils.getRid())) {
                int status = response.statusCode().value();
                log.info("PRE FILTER: STOP: {} --> {} {} {}", domain, status, method, requestUri);
            }
            return response;
        };
    }

    public static class FilterSupplier extends SimpleFilterSupplier {
        public FilterSupplier() {
            super(HighLogFilterFunctions.class);
        }
    }
}
