package com.icthh.xm.gate.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Properties specific to Xm Gate Java 25.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {

    private final Retry retry = new Retry();
    private final Gateway gateway = new Gateway();

    private List<String> hosts = new ArrayList<>();
    private boolean kafkaEnabled;
    private String kafkaSystemQueue;
    private String tenantPropertiesPathPattern;
    private String tenantPropertiesName;
    private Boolean disableIdpCookieUsage;
    private Boolean redirectToDefaultTenantEnabled;
    private MonitoringApi monitoring = new MonitoringApi();
    private String objectStorageFileRoot = "/";

    @Getter
    @Setter
    public static class MonitoringApi {
        private Enabled api;
    }

    @Getter
    @Setter
    public static class Enabled {
        private Boolean enabled;
    }

    @Getter
    @Setter
    private static class Retry {
        private int maxAttempts;
        private long delay;
        private int multiplier;
    }

    @Getter
    @Setter
    public static class Gateway {
        private Set<String> excludedServices = new HashSet<>();
    }
}
