package com.icthh.xm.gate.domain.idp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdpPublicConfig {

    @JsonProperty("idp")
    private IdpConfigContainer config;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdpConfigContainer {
        @JsonProperty("directLogin")
        private boolean directLogin;

        @JsonProperty("clients")
        private List<IdpPublicClientConfig> clients = new ArrayList<>();

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class IdpPublicClientConfig {

            @JsonProperty("key")
            private String key;

            @JsonProperty("name")
            private String name;

            @JsonProperty("clientId")
            private String clientId;

            @JsonProperty("redirectUri")
            private String redirectUri;

            @JsonProperty("features")
            private IdpPublicClientConfig.Features features;

            @JsonProperty("authorizationEndpoint")
            private IdpPublicClientConfig.AuthorizationEndpoint authorizationEndpoint;

            @JsonProperty("tokenEndpoint")
            private IdpPublicClientConfig.TokenEndpoint tokenEndpoint;

            @JsonProperty("userinfoEndpoint")
            private IdpPublicClientConfig.UserInfoEndpoint userinfoEndpoint;

            @JsonProperty("endSessionEndpoint")
            private IdpPublicClientConfig.BaseEndpoint endSessionEndpoint;

            @JsonProperty("jwksEndpoint")
            private IdpPublicClientConfig.BaseEndpoint jwksEndpoint;

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Features {
                @JsonProperty("pkce")
                private boolean pkce;

                @JsonProperty("stateful")
                private boolean stateful;

                @JsonProperty("bearirng")
                private IdpPublicClientConfig.Features.Bearirng bearirng;

                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Bearirng {
                    @JsonProperty("enabled")
                    private boolean enabled;

                    @JsonProperty("idpTokenHeader")
                    private String idpTokenHeader;

                    @JsonProperty("xmTokenHeader")
                    private String xmTokenHeader;
                }
            }

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            @EqualsAndHashCode(callSuper = true)
            public static class AuthorizationEndpoint extends IdpPublicClientConfig.BaseEndpoint {

                @JsonProperty("responseType")
                private String responseType;

                @JsonProperty("additionalParams")
                private Map<String, String> additionalParams;

                @JsonProperty("features")
                private AuthorizationEndpoint.Features features;

                @Data
                @JsonIgnoreProperties(ignoreUnknown = true)
                public static class Features {
                    @JsonProperty("state")
                    private boolean state;
                }
            }

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            @EqualsAndHashCode(callSuper = true)
            public static class TokenEndpoint extends IdpPublicClientConfig.BaseEndpoint {
                @JsonProperty("grantType")
                private String grantType;
            }

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class BaseEndpoint {
                @JsonProperty("uri")
                private String uri;
            }

            @Data
            @JsonIgnoreProperties(ignoreUnknown = true)
            @EqualsAndHashCode(callSuper = true)
            public static class UserInfoEndpoint extends IdpPublicClientConfig.BaseEndpoint {
                @JsonProperty("userNameAttributeName")
                private String userNameAttributeName;
            }
        }

    }
}
