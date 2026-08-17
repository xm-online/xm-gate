package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.gate.config.properties.ApplicationProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ServletRequestPathUtils;

import java.io.IOException;

import static com.icthh.xm.gate.config.Constants.FILTER_ORDER_API_PREFIX_STRIPPING;
import static com.icthh.xm.gate.utils.ServerRequestUtils.normalizeApiPrefix;
import static com.icthh.xm.gate.utils.ServerRequestUtils.stripApiPrefix;

/**
 * Strips the optional public api prefix (see {@code application.gateway.api-prefix}) from the request path.
 * <p>
 * Everything downstream works with the plain path: security matchers, access control and routing all read the
 * first path segment as the service name, and the gate's own endpoints ({@code /management/**}, {@code /api/**})
 * are mapped by Spring MVC on their plain paths. Removing the prefix once, before the Spring Security filter
 * chain and before the {@code DispatcherServlet}, keeps that assumption in a single place.
 * <p>
 * The prefix is optional: a request that arrives without it is passed through untouched, so
 * {@code /uaa/oauth/token} and {@code /xm-api/uaa/oauth/token} are served side by side even while the prefix
 * is configured.
 */
@Slf4j
@Component
@Order(FILTER_ORDER_API_PREFIX_STRIPPING)
public class ApiPrefixStrippingFilter extends OncePerRequestFilter {

    private final String apiPrefix;

    public ApiPrefixStrippingFilter(ApplicationProperties applicationProperties) {
        this.apiPrefix = normalizeApiPrefix(applicationProperties.getGateway().getApiPrefix());
        log.info("Api prefix stripping is {}", apiPrefix == null ? "disabled" : "enabled for [" + apiPrefix + "]");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String pathWithinApplication = pathWithinApplication(request);
        if (apiPrefix == null || pathWithinApplication.equals(stripApiPrefix(pathWithinApplication, apiPrefix))) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest strippedRequest = new ApiPrefixStrippedRequest(request, apiPrefix);
        log.debug("Api prefix [{}] stripped: {} -> {}", apiPrefix, request.getRequestURI(),
            strippedRequest.getRequestURI());

        // an earlier filter may have already parsed and cached the un-stripped path, re-parse it for the wrapper
        RequestPath cachedPath = ServletRequestPathUtils.hasParsedRequestPath(request)
            ? ServletRequestPathUtils.getParsedRequestPath(request)
            : null;
        if (cachedPath != null) {
            ServletRequestPathUtils.parseAndCache(strippedRequest);
        }
        try {
            filterChain.doFilter(strippedRequest, response);
        } finally {
            if (cachedPath != null) {
                ServletRequestPathUtils.setParsedRequestPath(cachedPath, request);
            }
        }
    }

    private static String pathWithinApplication(HttpServletRequest request) {
        return StringUtils.removeStart(request.getRequestURI(), request.getContextPath());
    }

    /**
     * Reports the request path with the api prefix removed. Only the path is rewritten, everything else
     * (headers, body, attributes, session) is delegated to the original request.
     */
    private static final class ApiPrefixStrippedRequest extends HttpServletRequestWrapper {

        private final String apiPrefix;

        private ApiPrefixStrippedRequest(HttpServletRequest request, String apiPrefix) {
            super(request);
            this.apiPrefix = apiPrefix;
        }

        @Override
        public String getRequestURI() {
            String contextPath = super.getContextPath();
            String pathWithinApplication = StringUtils.removeStart(super.getRequestURI(), contextPath);
            return contextPath + stripApiPrefix(pathWithinApplication, apiPrefix);
        }

        @Override
        public String getServletPath() {
            return stripApiPrefix(super.getServletPath(), apiPrefix);
        }

        @Override
        public StringBuffer getRequestURL() {
            StringBuffer url = super.getRequestURL();
            String originalUri = super.getRequestURI();
            int uriStart = url.length() - originalUri.length();
            if (uriStart < 0 || url.indexOf(originalUri, uriStart) != uriStart) {
                return url;
            }
            return new StringBuffer(url.substring(0, uriStart)).append(getRequestURI());
        }
    }
}
