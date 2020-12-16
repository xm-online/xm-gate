package com.icthh.xm.gate.domain.idp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdpPrivateConfig {

    @JsonProperty("idp")
    private IdpConfigContainer idpConfigContainer;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdpConfigContainer {

        @JsonProperty("clients")
        private List<IdpPrivateClientConfig> clients = new ArrayList<>();

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class IdpPrivateClientConfig {

            @JsonProperty("key")
            private String key;

            @JsonProperty("clientSecret")
            private String clientSecret;

            @JsonProperty("scope")
            private List<String> scope;

            @JsonProperty("additionalParams")
            private Map<String, String> additionalParams;

        }
    }
}
