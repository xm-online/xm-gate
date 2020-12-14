package com.icthh.xm.gate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdpDto {

    @JsonProperty("idp")
    private IdpConfigDto idp;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class IdpConfigDto {
        @JsonProperty("directLogin")
        private Boolean directLogin;

        @JsonProperty("clients")
        private List<IdpClientConfigDto> clients = new ArrayList<>();
    }
}
