package com.icthh.xm.gate.config.properties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Properties specific to Xm Gate Java 25.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {

    private final Retry retry = new Retry();
    private final Gateway gateway = new Gateway();
    private final CorsConfiguration cors = new CorsConfiguration();
    private final Security security = new Security();

    private List<String> hosts = new ArrayList<>();
    private boolean kafkaEnabled;
    private String kafkaSystemQueue;
    private String tenantPropertiesPathPattern;
    private String tenantPropertiesName;
    private Boolean disableIdpCookieUsage;
    private Boolean redirectToDefaultTenantEnabled;
    private MonitoringApi monitoring = new MonitoringApi();
    private String objectStorageFileRoot = "/";
    private HttpClient httpClient = new HttpClient();

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
        private Set<String> excludedServices = Set.of("consul", "gate");
        private Map<String, Set<String>> authorizedMicroservicesEndpoints = Map.of();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Security {
        private final ClientAuthorization clientAuthorization = new ClientAuthorization();

        @Getter
        @Setter
        @NoArgsConstructor
        public static class ClientAuthorization {
            private String accessTokenUri;
            private String tokenServiceId;
            private String clientId;
            private String clientSecret;
        }
    }

    @Getter
    @Setter
    public static class HttpClient {
        private Integer maxConnections;
        private Integer maxConnectionsPerRoute;
        private Integer connectionTimeoutSeconds;
    }
}
