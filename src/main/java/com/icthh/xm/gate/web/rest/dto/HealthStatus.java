package com.icthh.xm.gate.web.rest.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Data;
import org.springframework.boot.actuate.health.Status;

@Data
@Builder
public class HealthStatus {

    private String status;
    private Map<String, Object> details;
}
