package com.icthh.xm.gate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdpClientConfigDto {

    @JsonProperty("key")
    private String key;

    @JsonProperty("name")
    private String name;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("redirectUri")
    private String redirectUri;

    @JsonProperty("features")
    private Features features;

    @JsonProperty("authorizationEndpoint")
    private AuthorizationEndpoint authorizationEndpoint;

    @JsonProperty("tokenEndpoint")
    private TokenEndpoint tokenEndpoint;

    @JsonProperty("userinfoEndpoint")
    private BaseEndpoint userinfoEndpoint;

    @JsonProperty("endSessionEndpoint")
    private BaseEndpoint endSessionEndpoint;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class Features {
        @JsonProperty("pkce")
        private Boolean pkce;

        @JsonProperty("stateful")
        private Boolean stateful;

        @JsonProperty("bearirng")
        private Bearirng bearirng;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public class Bearirng {
            @JsonProperty("enabled")
            private Boolean enabled;

            @JsonProperty("idpTokenHeader")
            private String idpTokenHeader;

            @JsonProperty("xmTokenHeader")
            private String xmTokenHeader;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @EqualsAndHashCode(callSuper = true)
    public class AuthorizationEndpoint extends BaseEndpoint {

        @JsonProperty("responseType")
        private String responseType;

        @JsonProperty("xmTokenHeader")
        private String xmTokenHeader;

        @JsonProperty("additionalParams")
        private Map<String, String> additionalParams;

        @JsonProperty("features")
        private Features features;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        private class Features {
            @JsonProperty("state")
            private Boolean state;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @EqualsAndHashCode(callSuper = true)
    public class TokenEndpoint extends BaseEndpoint {
        @JsonProperty("grantType")
        private String grantType;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class BaseEndpoint {
        @JsonProperty("uri")
        private String uri;
    }
}
