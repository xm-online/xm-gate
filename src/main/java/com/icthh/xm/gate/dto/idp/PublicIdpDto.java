package com.icthh.xm.gate.dto.idp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicIdpDto {

    @JsonProperty("idp")
    private PublicIdpConfigDto idp;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class PublicIdpConfigDto {
        @JsonProperty("directLogin")
        private Boolean directLogin;

        @JsonProperty("clients")
        private List<PublicIdpClientConfigDto> clients = new ArrayList<>();
    }
}
