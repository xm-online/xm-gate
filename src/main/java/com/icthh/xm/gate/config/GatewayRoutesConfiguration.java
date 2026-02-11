package com.icthh.xm.gate.config;

import com.icthh.xm.gate.config.properties.ApplicationProperties;
import com.icthh.xm.gate.gateway.functions.AccessControlFilterFunctions;
import com.icthh.xm.gate.gateway.functions.AddDomainRelayHeadersFunctions;
import com.icthh.xm.gate.gateway.functions.HighLogFilterFunctions;
import com.icthh.xm.gate.gateway.functions.IdpStatefulModeFilterFunctions;
import com.icthh.xm.gate.gateway.functions.LoggingFilterFunctions;
import com.icthh.xm.gate.gateway.functions.TfaTokenDetectionFilterFunctions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Set;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

/**
 * Gateway Routes Configuration.
 * Dynamically routes requests to microservices discovered from Consul.
 */
@Profile("client-discovery")
@Slf4j
@Configuration
public class GatewayRoutesConfiguration {

    private final DiscoveryClient discoveryClient;
    private final Set<String> excludedServices;

    public GatewayRoutesConfiguration(DiscoveryClient discoveryClient,
                                      ApplicationProperties applicationProperties) {
        this.discoveryClient = discoveryClient;
        this.excludedServices = Set.copyOf(applicationProperties.getGateway().getExcludedServices());
    }

    @Bean
    public RouterFunction<ServerResponse> dynamicServiceRoutes() {
        var services = discoveryClient.getServices();

        log.info("Discovered services from Consul: {}", services);
        log.info("Excluded services from routing: {}", excludedServices);

        return services.stream()
            .filter(serviceId -> !excludedServices.contains(serviceId))
            .map(this::createServiceRoute)
            .reduce(RouterFunction::and)
            .orElseGet(this::emptyRoute);
    }

    private RouterFunction<ServerResponse> createServiceRoute(String serviceId) {
        log.info("Created route for service: {} -> /{}/**", serviceId, serviceId);
        return route(serviceId)
            .route(path("/" + serviceId + "/**"), http())
            .filter(lb(serviceId))
            .before(BeforeFilterFunctions.stripPrefix(1))
            .filter(HighLogFilterFunctions.addHighLog())
            .filter(LoggingFilterFunctions.addLogging())
            .filter(TfaTokenDetectionFilterFunctions.tfaTokenDetection())
            .filter(AccessControlFilterFunctions.accessControl())
            .filter(AddDomainRelayHeadersFunctions.addDomainRelayHeaders())
            .filter(IdpStatefulModeFilterFunctions.idpStatefulMode())
            .build();
    }

    private RouterFunction<ServerResponse> emptyRoute() {
        log.warn("No services discovered from Consul or all services are excluded");
        return route("fallback")
            .route(path("/no-routes-configured"), http())
            .build();
    }
}
