package com.icthh.xm.gate.gateway.ratelimiting;

import com.icthh.xm.gate.utils.ServerRequestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static com.icthh.xm.gate.config.Constants.DEFAULT_TENANT;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

@Configuration
public class RateLimitingConfiguration {

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    @Primary
    public KeyResolver tenantKeyResolver() {
        return exchange -> Mono.just(getTenantKey(exchange.getRequest()));
    }

    @Bean
    public KeyResolver tenantClientKeyResolver() {
        return exchange -> Mono.just(getClientIdKey(exchange.getRequest()));
    }

    private String getClientIdKey(ServerHttpRequest request) {
        String tenantKey = getTenantKey(request);
        String jwtToken = request.getHeaders().getFirst(AUTHORIZATION);
        String clientId = ServerRequestUtils.getClientIdFromToken(jwtToken);
        return StringUtils.isBlank(clientId) ? tenantKey : tenantKey + ":" + clientId;
    }

    public String getTenantKey(ServerHttpRequest request) {
        return Optional.ofNullable(request.getHeaders().getFirst(HEADER_TENANT)).orElse(DEFAULT_TENANT);
    }
}

