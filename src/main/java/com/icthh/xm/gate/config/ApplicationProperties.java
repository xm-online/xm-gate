package com.icthh.xm.gate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Properties specific to JHipster.
 * <p>
 * Properties are configured in the application.yml file.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Retry retry = new Retry();

    private List<String> hosts = new ArrayList<>();
    private boolean kafkaEnabled;
    private String kafkaSystemQueue;
    private String clientId;
    private boolean cassandraEnabled;
    private Map<String, RateLimiting> rateLimiting = new HashMap<>();

    @Getter
    @Setter
    private static class Retry {

        private int maxAttempts;
        private long delay;
        private int multiplier;
    }

    @Getter
    @Setter
    public static class RateLimiting {

        private long limit = 100_000L;
        private int durationInSeconds = 3_600;
    }
}
