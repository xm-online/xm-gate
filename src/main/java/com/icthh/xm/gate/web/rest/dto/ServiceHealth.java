package com.icthh.xm.gate.web.rest.dto;

import com.icthh.xm.gate.domain.health.HealthResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceHealth {

    private String instanceId;
    private HealthResponse health;
}
