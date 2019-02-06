package com.icthh.xm.gate.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenHolder {
    private String systemAuthUrl;
    private String systemUsername;
    private String systemPassword;
    private String systemClientToken;
}
