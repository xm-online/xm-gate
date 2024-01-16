package com.icthh.xm.gate.gateway.ratelimiting;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

import static com.icthh.xm.gate.config.Constants.DEFAULT_TENANT;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.CLIENT_ID;

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
    public KeyResolver clientKeyResolver() {
        return exchange -> Mono.just(getClientIdFromToken(exchange.getRequest()));
    }

    private String getClientIdFromToken(ServerHttpRequest request) {
        String jwtToken = Objects.requireNonNull(request.getHeaders().get(AUTHORIZATION)).get(0);
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder().setSkipSignatureVerification().build();
            String tenantKey = getTenantKey(request);
            String clientId = jwtConsumer.processToClaims(jwtToken.replace("Bearer ", "")).getClaimValueAsString(CLIENT_ID);
            return tenantKey + ":" + clientId;
        } catch (InvalidJwtException e) {
            throw new RuntimeException(e);
        }
    }

    public String getTenantKey(ServerHttpRequest request) {
        return Optional.ofNullable(request.getHeaders().getFirst(HEADER_TENANT)).orElse(DEFAULT_TENANT);
    }
}

