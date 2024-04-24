package com.icthh.xm.gate.gateway.ratelimiting;

import com.icthh.xm.gate.config.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class XmRateLimiter extends RedisRateLimiter {

    @Value("${spring.cloud.gateway.discovery.locator.route-id-prefix}")
    private String DISCOVERY_CLIENT_PREFIX = "ReactiveCompositeDiscoveryClient_";
    private static final String DEFAULT_FILTERS_KEY = "defaultFilters";

    private final ApplicationProperties applicationProperties;
    private final String keyResolverName;

    public XmRateLimiter(ReactiveStringRedisTemplate redisTemplate, RedisScript<List<Long>> script,
                         ConfigurationService configurationService, ApplicationProperties applicationProperties,
                         String keyResolverName) {
        super(redisTemplate, script, configurationService);
        this.applicationProperties = applicationProperties;
        this.keyResolverName = keyResolverName;
    }

    @Override
    public Map<String, Config> getConfig() {
        Map<String, Config> configMap = getActualValidProperties().stream()
            .collect(Collectors.toMap(p -> DISCOVERY_CLIENT_PREFIX + p.getRouteId(), this::buildRateLimiterConfig));

        configMap.putIfAbsent(DEFAULT_FILTERS_KEY, getDefaultRateLimiterConfig());

        return configMap;
    }

    private Set<ApplicationProperties.RedisRateLimiterProperties> getActualValidProperties() {
        List<ApplicationProperties.RedisRateLimiterProperties> redisRateLimiterCfg =
            applicationProperties.getRedisRateLimiter() == null ? List.of() : applicationProperties.getRedisRateLimiter();
        return redisRateLimiterCfg
            .stream()
            .filter(prop -> StringUtils.isNotEmpty(prop.getRouteId()))
            .filter(prop -> StringUtils.isNotEmpty(prop.getKeyResolver()))
            .filter(prop -> keyResolverName.equals(prop.getKeyResolver()))
            .collect(Collectors.toSet());
    }

    private Config buildRateLimiterConfig(ApplicationProperties.RedisRateLimiterProperties properties) {
        RedisRateLimiter.Config config = new RedisRateLimiter.Config();
        config.setReplenishRate(properties.getReplenishRate());
        config.setBurstCapacity(properties.getBurstCapacity());
        config.setRequestedTokens(properties.getRequestedTokens());
        return config;
    }

    private Config getDefaultRateLimiterConfig() {
        RedisRateLimiter.Config config = new RedisRateLimiter.Config();
        config.setReplenishRate(50);
        config.setBurstCapacity(50);
        config.setRequestedTokens(1);
        return config;
    }
}
