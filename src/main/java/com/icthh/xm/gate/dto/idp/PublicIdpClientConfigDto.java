package com.icthh.xm.gate.dto.idp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
//TODO PublicIdpClientConfigDto -> IdpPublicClientConfig
//TODO let's make it as inner class of IdpPublicConfig
public class PublicIdpClientConfigDto {

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
    //TODO Inner class may be static (apply this for all nestated classes)
    public class Features {
        @JsonProperty("pkce")
        //TODO use primitive object boolean  (if we use class Boolean we add one more state pkce=null)
        //TODO apply this everywhere
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
