package com.icthh.xm.gate.gateway.ratelimiting;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import reactor.core.publisher.Mono;

@Configuration
@ConfigurationProperties(prefix = "spring.cloud.gateway.redis-rate-limiter")
@Setter
public class RateLimitingConfiguration {

    private Integer replenishRate;
    private Integer burstCapacity;
    private Integer requestedTokens;

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(replenishRate, burstCapacity, requestedTokens);
    }

    @Bean
    KeyResolver keyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getURI().getPath());
    }

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("uaa", p -> p
                .path("/uaa/**")
                .filters(f -> f.stripPrefix(1)
                    .requestRateLimiter(r -> r.setRateLimiter(redisRateLimiter())
                    .setDenyEmptyKey(true)
                    .setKeyResolver(keyResolver())))
                .uri("http://uaa:9999"))
            .build();
    }

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        return new LettuceConnectionFactory();
    }
}
