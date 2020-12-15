package com.icthh.xm.gate.dto.idp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrivateIdpClientConfigDto {

    @JsonProperty("key")
    private String key;

    @JsonProperty("clientSecret")
    private String clientSecret;

    @JsonProperty("scope")
    private List<String> scope;

    @JsonProperty("additionalParams")
    private Map<String, String> additionalParams;

}
