package com.icthh.xm.gate.gateway.ratelimiting;

import com.icthh.xm.gate.config.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.icthh.xm.gate.config.Constants.BURST_CAPACITY;
import static com.icthh.xm.gate.config.Constants.DENY_EMPTY;
import static com.icthh.xm.gate.config.Constants.FILTER_NAME;
import static com.icthh.xm.gate.config.Constants.KEY_RESOLVER;
import static com.icthh.xm.gate.config.Constants.REPLENISH_RATE;
import static com.icthh.xm.gate.config.Constants.REQUESTED_TOKENS;
import static com.icthh.xm.gate.utils.RouteUtils.clearRouteId;
import static java.util.stream.Collectors.groupingBy;

/**
 * This class is used to enrich routes, discovered by consul, with RequestRateLimiter filter.
 * Route will be supplemented if the appropriate configuration is available for this route.
 */
@Slf4j
@Component
public class DiscoveryClientRouteDefinitionLocatorEnhancer extends DiscoveryClientRouteDefinitionLocator {

    private final Map<String, List<ApplicationProperties.RedisRateLimiterProperties>> rateLimitFilterConfig;

    public DiscoveryClientRouteDefinitionLocatorEnhancer(ReactiveDiscoveryClient discoveryClient,
                                                         DiscoveryLocatorProperties properties,
                                                         ApplicationProperties applicationProperties) {
        super(discoveryClient, properties);

        this.rateLimitFilterConfig = Optional.ofNullable(applicationProperties.getRedisRateLimiter())
            .stream()
            .flatMap(Collection::stream)
            .filter(route -> StringUtils.isNotEmpty(route.getRouteId()))
            .collect(groupingBy(ApplicationProperties.RedisRateLimiterProperties::getRouteId));

        log.info("Configured {} routes", this.rateLimitFilterConfig.keySet());
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return super.getRouteDefinitions().map(this::enrichRouteDefinition);
    }

    private RouteDefinition enrichRouteDefinition(RouteDefinition routeDefinition) {
        String routeId = clearRouteId(routeDefinition.getId()); // ReactiveCompositeDiscoveryClient adds prefix to the route IDs to avoid naming conflicts and clearly indicate their origin

        List<ApplicationProperties.RedisRateLimiterProperties> properties = rateLimitFilterConfig.getOrDefault(routeId, List.of());

        log.info("for route={} found={} properties", routeDefinition.getId(), properties.size());

        properties.forEach(rateLimiter -> addRateLimitingFilter(routeId, routeDefinition, rateLimiter));

        return routeDefinition;
    }

    private void addRateLimitingFilter(String routeId, RouteDefinition routeDefinition,
                                       ApplicationProperties.RedisRateLimiterProperties properties) {
        FilterDefinition filterDefinition = buildRateLimiterFilter(properties);
        routeDefinition.getFilters().add(filterDefinition);

        log.info("Successfully added RequestRateLimiter filter with parameters: replenishRate = {}, "
            + "burstCapacity = {}, requestedTokens = {}, key-resolver = {}, deny-empty-key = {} to route with id: {}",
            properties.getReplenishRate(), properties.getBurstCapacity(), properties.getRequestedTokens(),
            properties.getKeyResolver(), properties.getDenyEmpty(), routeId);
    }

    private static FilterDefinition buildRateLimiterFilter(ApplicationProperties.RedisRateLimiterProperties args) {
        FilterDefinition filterDefinition = new FilterDefinition();
        filterDefinition.setName(FILTER_NAME);
        filterDefinition.addArg(REPLENISH_RATE, String.valueOf(args.getReplenishRate()));
        filterDefinition.addArg(BURST_CAPACITY, String.valueOf(args.getBurstCapacity()));
        filterDefinition.addArg(REQUESTED_TOKENS, String.valueOf(args.getRequestedTokens()));
        filterDefinition.addArg(KEY_RESOLVER, args.getKeyResolver());
        filterDefinition.addArg(DENY_EMPTY, args.getDenyEmpty());

        return filterDefinition;
    }
}
