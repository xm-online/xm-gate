package com.icthh.xm.gate.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Properties specific to Gate.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {

    private final Retry retry = new Retry();

    private List<String> hosts = new ArrayList<>();

    private boolean kafkaEnabled;
    private String kafkaSystemQueue;
    private String tenantPropertiesPathPattern;
    private String tenantPropertiesName;
    private String tenantPropertiesDomainsConfigKey;
    private String tenantPropertiesListConfigKey;

    private String specificationFolderPathPattern;
    private String specificationPathPattern;
    private String specificationName;

    private List<RedisRateLimiterProperties> redisRateLimiter;

    @Getter
    @Setter
    private static class Retry {

        private int maxAttempts;
        private long delay;
        private int multiplier;

    }

    @Getter
    @Setter
    public static class RedisRateLimiterProperties {
        @NotEmpty
        private String routeId;
        private int replenishRate = 10;
        private int burstCapacity = 10;
        private int requestedTokens = 1;
        @NotEmpty
        private String keyResolver;
        private String denyEmpty = "true";
    }

}
