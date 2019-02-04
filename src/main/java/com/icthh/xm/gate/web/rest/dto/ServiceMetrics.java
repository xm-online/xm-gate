package com.icthh.xm.gate.web.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ServiceMetrics {

    private String instanceId;
    private Map<String, Object> value;
}
