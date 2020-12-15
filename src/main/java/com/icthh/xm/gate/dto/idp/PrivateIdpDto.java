package com.icthh.xm.gate.dto.idp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrivateIdpDto {

    @JsonProperty("idp")
    private PrivateIdpConfigDto idp;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class PrivateIdpConfigDto {

        @JsonProperty("clients")
        private List<PrivateIdpClientConfigDto> clients = new ArrayList<>();
    }
}
