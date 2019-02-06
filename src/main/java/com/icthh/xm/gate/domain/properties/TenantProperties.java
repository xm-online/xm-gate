package com.icthh.xm.gate.domain.properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.icthh.xm.gate.domain.TokenHolder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class TenantProperties {

    private RateLimiting rateLimiting = new RateLimiting();
    private TokenHolder token = new TokenHolder();

    @Getter
    @Setter
    public static class RateLimiting {

        private Map<String, RateLimitingConf> oauth2Client = new HashMap<>();

        @Getter
        @Setter
        public static class RateLimitingConf {
            private long limit;
            private int durationInSeconds;
        }
    }
}
