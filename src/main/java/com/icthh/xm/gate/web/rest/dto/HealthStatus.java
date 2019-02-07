package com.icthh.xm.gate.web.rest.dto;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HealthStatus {

    private String status;
    private Map<String, Object> details;
}
