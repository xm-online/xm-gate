package com.icthh.xm.gate.web.rest.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.boot.actuate.health.Health;

@Data
@Builder
public class ServiceHealth {

    private String instanceId;
    private HealthStatus health;
}
