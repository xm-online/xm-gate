package com.icthh.xm.gate.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TenantConfig {
    private UaaConfig uaa;
}
