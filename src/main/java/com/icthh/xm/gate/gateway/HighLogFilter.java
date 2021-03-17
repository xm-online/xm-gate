package com.icthh.xm.gate.gateway;

import com.icthh.xm.commons.logging.util.MdcUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter for logging all HTTP requests with highest precedence
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HighLogFilter implements Filter {

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
                         final FilterChain chain) throws IOException, ServletException {

        String ridValue = "";
        String domain = null;
        String method = null;
        String requestUri = null;

        try {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest req = (HttpServletRequest) request;

                domain = request.getServerName();
                method = req.getMethod();
                requestUri = req.getRequestURI();
                ridValue = MdcUtils.generateRid();
                MdcUtils.putRid(ridValue);

                log.info("PRE FILTER: START: {} --> {} {}", domain, method, requestUri);
            }
        } catch (Exception e) {
            log.warn("Error during RiD processing: {}", e.getMessage(), e);
        }

        chain.doFilter(request, response);

        // if Rid was not modified by standard LoggingFilter we need to log the http status
        if (ridValue.equals(MdcUtils.getRid())) {
            Integer status = null;
            if (response instanceof HttpServletResponse) {
                status = ((HttpServletResponse) response).getStatus();
            }
            log.info("PRE FILTER:  STOP: {} --> {} {} {}", domain, status, method, requestUri);
        }
    }

}
