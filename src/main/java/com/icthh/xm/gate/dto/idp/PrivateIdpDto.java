package com.icthh.xm.gate.dto.idp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
//TODO let's move this class to  com.icthh.xm.gate.domain.idp
//TODO PrivateIdpDto -> IdpPrivateConfig
public class PrivateIdpDto {

    @JsonProperty("idp") //TODO can we map without this inner class?
    private PrivateIdpConfigDto idp;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public class PrivateIdpConfigDto {

        @JsonProperty("clients")
        private List<PrivateIdpClientConfigDto> clients = new ArrayList<>();
    }
}
