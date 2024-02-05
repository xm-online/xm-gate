package com.icthh.xm.gate.gateway.ratelimiting;

import com.icthh.xm.gate.config.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import static com.icthh.xm.gate.config.Constants.BURST_CAPACITY;
import static com.icthh.xm.gate.config.Constants.DENY_EMPTY;
import static com.icthh.xm.gate.config.Constants.FILTER_NAME;
import static com.icthh.xm.gate.config.Constants.KEY_RESOLVER;
import static com.icthh.xm.gate.config.Constants.REPLENISH_RATE;
import static com.icthh.xm.gate.config.Constants.REQUESTED_TOKENS;
import static com.icthh.xm.gate.utils.RouteUtils.clearRouteId;

/**
 * This class is used to enrich routes, discovered by consul, with RequestRateLimiter filter.
 * Route will be supplemented if the appropriate configuration is available for this route.
 */
@Slf4j
@Component
public class DiscoveryClientRouteDefinitionLocatorEnhancer extends DiscoveryClientRouteDefinitionLocator {

    private final ApplicationProperties applicationProperties;

    public DiscoveryClientRouteDefinitionLocatorEnhancer(ReactiveDiscoveryClient discoveryClient,
                                                         DiscoveryLocatorProperties properties,
                                                         ApplicationProperties applicationProperties) {
        super(discoveryClient, properties);
        this.applicationProperties = applicationProperties;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return super.getRouteDefinitions().map(this::enrichRouteDefinition);
    }

    private RouteDefinition enrichRouteDefinition(RouteDefinition routeDefinition) {
        String routeId = clearRouteId(routeDefinition.getId()); // ReactiveCompositeDiscoveryClient adds prefix to the route IDs to avoid naming conflicts and clearly indicate their origin
        var redisRateLimiterProperties = applicationProperties.getRedisRateLimiter();

        if (Objects.nonNull(redisRateLimiterProperties)) {
            Optional.ofNullable(redisRateLimiterProperties.get(routeId)).orElse(Collections.emptyList())
                .forEach(properties -> addRateLimitingFilter(routeId, routeDefinition, properties));
        }
        return routeDefinition;
    }

    private void addRateLimitingFilter(String routeId, RouteDefinition routeDefinition,
                                       ApplicationProperties.RedisRateLimiterProperties properties) {
        FilterDefinition filterDefinition = buildRateLimiterFilter(properties);
        routeDefinition.getFilters().add(filterDefinition);

        log.debug("Successfully added RequestRateLimiter filter with parameters: replenishRate = {}, "
            + "burstCapacity = {}, requestedTokens = {}, key-resolver = {}, deny-empty-key = {} to route with id: {}",
            properties.getReplenishRate(), properties.getBurstCapacity(), properties.getRequestedTokens(),
            properties.getKeyResolver(), properties.getDenyEmpty(), routeId);
    }

    private static FilterDefinition buildRateLimiterFilter(ApplicationProperties.RedisRateLimiterProperties args) {
        FilterDefinition filterDefinition = new FilterDefinition();
        filterDefinition.setName(FILTER_NAME);
        filterDefinition.addArg(REPLENISH_RATE, args.getReplenishRate());
        filterDefinition.addArg(BURST_CAPACITY, args.getBurstCapacity());
        filterDefinition.addArg(REQUESTED_TOKENS, args.getRequestedTokens());
        filterDefinition.addArg(KEY_RESOLVER, args.getKeyResolver());
        filterDefinition.addArg(DENY_EMPTY, args.getDenyEmpty());

        return filterDefinition;
    }
}
