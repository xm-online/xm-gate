package com.icthh.xm.gate.gateway.accesscontrol;

import com.icthh.xm.gate.utils.ServerRequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tech.jhipster.config.JHipsterProperties;

import java.util.List;
import java.util.Map;

import static com.icthh.xm.gate.utils.RouteUtils.clearRouteId;

@Slf4j
@Component
public final class OpenPolicyAgentAuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final JHipsterProperties jHipsterProperties;
    private final RouteLocator routeLocator;

    public OpenPolicyAgentAuthorizationManager(JHipsterProperties jHipsterProperties, RouteLocator routeLocator) {
        this.jHipsterProperties = jHipsterProperties;
        this.routeLocator = routeLocator;
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, AuthorizationContext context) {
        ServerHttpRequest request = context.getExchange().getRequest();

        String requestUri = request.getPath().pathWithinApplication().value();
        String serviceName = ServerRequestUtils.getServiceNameFromRequestPath(request);

        if (StringUtils.isBlank(serviceName)) {
            return Mono.just(new AuthorizationDecision(false));
        }

        return routeLocator.getRoutes()
            .any(route -> clearRouteId(route.getId()).equals(serviceName) && isAuthorizedRequest(serviceName, requestUri))
            .map(AuthorizationDecision::new);
    }

    private boolean isAuthorizedRequest(String serviceName, String requestUri) {
        Map<String, List<String>> authorizedMicroservicesEndpoints = jHipsterProperties.getGateway()
            .getAuthorizedMicroservicesEndpoints();

        // If the authorized endpoints list was left empty for this route, all access are allowed
        if (authorizedMicroservicesEndpoints.get(serviceName) == null) {
            log.debug("Access Control: allowing access for {}, as no access control policy has been set up for " +
                "service: {}", requestUri, serviceName);
            return true;
        } else {
            List<String> authorizedEndpoints = authorizedMicroservicesEndpoints.get(serviceName);

            // Go over the authorized endpoints to control that the request URI matches it
            for (String endpoint : authorizedEndpoints) {
                String gatewayEndpoint = "/" + serviceName + endpoint;
                // If the request Uri does not start with the path of the authorized endpoints, we block the request
                if (requestUri.startsWith(gatewayEndpoint)) {
                    log.debug("Access Control: allowing access for {}, as it matches the following authorized " +
                        "microservice endpoint: {}", requestUri, gatewayEndpoint);
                    return true;
                }
            }
        }
        return false;
    }
}
