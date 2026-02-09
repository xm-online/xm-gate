package com.icthh.xm.gate.gateway.functions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import tech.jhipster.config.JHipsterProperties;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.gate.gateway.ratelimitting.ServerRequestUtils.extractPathWithinService;
import static com.icthh.xm.gate.gateway.ratelimitting.ServerRequestUtils.extractServiceName;

/**
 * Filter for restricting access to backend microservices endpoints.
 * Uses Consul service discovery to validate services and custom gateway properties for access control.
 */
@Slf4j
public class AccessControlFilterFunctions {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public static HandlerFilterFunction<ServerResponse, ServerResponse> accessControl() {
        return (request, next) -> {
            HttpServletRequest servletRequest = request.servletRequest();

            DiscoveryClient discoveryClient = MvcUtils.getApplicationContext(request)
                .getBean(DiscoveryClient.class);
            JHipsterProperties jHipsterProperties = MvcUtils.getApplicationContext(request)
                .getBean(JHipsterProperties.class);

            String requestUri = servletRequest.getRequestURI();
            String serviceName = extractServiceName(requestUri);

            if (serviceName == null || serviceName.isEmpty()) {
                log.debug("Access Control: could not determine service name for {}", requestUri);
                return next.handle(request);
            }

            if (!isRegisteredService(discoveryClient, serviceName)) {
                log.warn("Access Control: unknown service requested: {}", serviceName);
                return next.handle(request);
            }

            if (isAuthorizedEndpoint(jHipsterProperties, serviceName, requestUri)) {
                return next.handle(request);
            }

            log.debug("Access Control: filtered unauthorized access on endpoint {}", requestUri);
            return ServerResponse.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AccessDeniedException("You are not authorized to access this resource").toString());
        };
    }

    private static boolean isRegisteredService(DiscoveryClient discoveryClient, String serviceName) {
        return discoveryClient.getServices()
            .stream()
            .anyMatch(s -> s.equalsIgnoreCase(serviceName));
    }

    private static boolean isAuthorizedEndpoint(JHipsterProperties jHipsterProperties, String serviceName, String requestUri) {
        Map<String, List<String>> authEndpoints = jHipsterProperties.getGateway().getAuthorizedMicroservicesEndpoints();

        if (authEndpoints == null || authEndpoints.isEmpty()) {
            log.debug("Access Control: access control policy has not been configured");
            return true;
        }

        List<String> allowedPatterns = authEndpoints.get(serviceName);
        if (allowedPatterns == null || allowedPatterns.isEmpty()) {
            log.debug("Access Control: allowing access for {}, as no access control policy has been set up for service: {}",
                requestUri, serviceName);
            return true;
        }

        String pathWithinService = extractPathWithinService(requestUri, serviceName);
        for (String pattern : allowedPatterns) {
            if (PATH_MATCHER.match(pattern, pathWithinService)) {
                log.debug("Access Control: allowing access for {}, as it matches authorized endpoint pattern: {}",
                    requestUri, pattern);
                return true;
            }
        }

        log.debug("Access Control: denying access for {}, no matching authorized endpoint for service: {}",
            requestUri, serviceName);
        return false;
    }

    public static class FilterSupplier extends SimpleFilterSupplier {
        public FilterSupplier() {
            super(AccessControlFilterFunctions.class);
        }
    }
}
