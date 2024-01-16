package com.icthh.xm.gate.gateway.ratelimiting;

import lombok.Setter;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.CLIENT_ID;

@Configuration
@Setter
public class RateLimitingConfiguration {

    @Bean
    @Primary
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    @Primary
    public KeyResolver addressKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getURI().getPath());
    }

    @Bean
    public KeyResolver clientKeyResolver() {
        return exchange -> Mono.just(Objects.requireNonNull(exchange.getRequest().getHeaders().get(AUTHORIZATION)))
            .map(p -> getClientIdFromToken(p.get(0)));
    }

    private String getClientIdFromToken(String jwtToken) {
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder().setSkipSignatureVerification().build();
            return jwtConsumer.processToClaims(jwtToken.replace("Bearer ", "")).getClaimValueAsString(CLIENT_ID);
        } catch (InvalidJwtException e) {
            throw new RuntimeException(e);
        }
    }
}

