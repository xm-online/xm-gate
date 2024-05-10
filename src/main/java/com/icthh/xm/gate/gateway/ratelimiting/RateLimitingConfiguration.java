package com.icthh.xm.gate.gateway.ratelimiting;

import com.icthh.xm.gate.config.ApplicationProperties;
import com.icthh.xm.gate.utils.ServerRequestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static com.icthh.xm.gate.config.Constants.DEFAULT_TENANT;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

@Configuration
public class RateLimitingConfiguration {

    @Bean
    @Primary
    public KeyResolver tenantKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest()).map(r -> getRouteIdPrefix(r) + getTenantKey(r));
    }

    @Bean
    public KeyResolver tenantClientKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest()).map(r -> getRouteIdPrefix(r) + getClientIdKey(r));
    }

    @Bean
    public KeyResolver serviceSessionIdKeyResolver() {
        return exchange -> getSessionIdKey(exchange.getRequest());
    }

    @Primary
    @Bean
    public XmRateLimiter tenantKeyRateLimiter(ReactiveStringRedisTemplate redisTemplate, RedisScript<List<Long>> script,
                                              ConfigurationService configurationService, ApplicationProperties applicationProperties) {
        return new XmRateLimiter(redisTemplate, script, configurationService, applicationProperties, "tenantKeyResolver");
    }

    @Bean
    public XmRateLimiter tenantClientKeyRateLimiter(ReactiveStringRedisTemplate redisTemplate, RedisScript<List<Long>> script,
                                                    ConfigurationService configurationService, ApplicationProperties applicationProperties) {
        return new XmRateLimiter(redisTemplate, script, configurationService, applicationProperties, "tenantClientKeyResolver");
    }

    @Bean
    public XmRateLimiter serviceSessionIdRateLimiter(ReactiveStringRedisTemplate redisTemplate, RedisScript<List<Long>> script,
                                                     ConfigurationService configurationService, ApplicationProperties applicationProperties) {
        return new XmRateLimiter(redisTemplate, script, configurationService, applicationProperties, "serviceSessionIdRateLimiter");
    }

    private Mono<String> getSessionIdKey(ServerHttpRequest request) {
        String sessionId = request.getHeaders().getFirst(ServerRequestUtils.SESSION_ID_HEADER);
        String serviceName = getRouteIdPrefix(request);
        // if sessionId is blank, return Mono.empty() to disable rate limiting by this key
        return StringUtils.isNotBlank(sessionId) ? Mono.just(serviceName + sessionId) : Mono.empty();
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

    public String getRouteIdPrefix(ServerHttpRequest request) {
        return ServerRequestUtils.getServiceNameFromRequestPath(request) + ":";
    }
}

