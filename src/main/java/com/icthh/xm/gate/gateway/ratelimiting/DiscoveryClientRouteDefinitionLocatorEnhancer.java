package com.icthh.xm.gate.gateway.ratelimiting;

import com.icthh.xm.gate.config.ApplicationProperties;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Objects;

import static com.icthh.xm.gate.utils.RouteUtils.clearRouteId;

@Component
public class DiscoveryClientRouteDefinitionLocatorEnhancer extends DiscoveryClientRouteDefinitionLocator {

    private static final String FILTER_NAME = "RequestRateLimiter";
    private static final String REPLENISH_RATE = "redis-rate-limiter.replenishRate";
    private static final String BURST_CAPACITY = "redis-rate-limiter.burstCapacity";
    private static final String REQUESTED_TOKENS = "redis-rate-limiter.requestedTokens";
    private static final String KEY_RESOLVER = "key-resolver";

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
        var redisRateLimiterProperties = applicationProperties.getRedisRateLimiter().get(routeId);

        if (Objects.nonNull(redisRateLimiterProperties)) {
            redisRateLimiterProperties.stream()
                .map(DiscoveryClientRouteDefinitionLocatorEnhancer::buildRateLimiterFilter)
                .forEach(f -> routeDefinition.getFilters().add(f));
        }
        return routeDefinition;
    }

    private static FilterDefinition buildRateLimiterFilter(ApplicationProperties.RedisRateLimiterProperties args) {
        FilterDefinition filterDefinition = new FilterDefinition();
        filterDefinition.setName(FILTER_NAME);
        filterDefinition.addArg(REPLENISH_RATE, args.getReplenishRate());
        filterDefinition.addArg(BURST_CAPACITY, args.getBurstCapacity());
        filterDefinition.addArg(REQUESTED_TOKENS, args.getRequestedTokens());
        filterDefinition.addArg(KEY_RESOLVER, args.getKeyResolver());

        return filterDefinition;
    }
}
