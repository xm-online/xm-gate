package com.icthh.xm.gate.domain.properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class TenantProperties {

    private Map<String, RateLimiting> rateLimiting = new HashMap<>();

    @Getter
    @Setter
    public static class RateLimiting {

        private long limit;
        private int durationInSeconds;
    }
}
