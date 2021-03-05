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

        try {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest req = (HttpServletRequest) request;

                String domain = request.getServerName();
                String method = req.getMethod();
                String requestUri = req.getRequestURI();
                MdcUtils.putRid(MdcUtils.generateRid());

                log.info("PRE FILTER {} --> {} {}", domain, method, requestUri);
            }
        } catch (Exception e) {
            log.warn("Error during RiD processing: {}", e.getMessage(), e);
        }

        chain.doFilter(request, response);
    }

}
