package com.icthh.xm.gate.web.rest.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;

@Data
@Builder
public class ServiceMetrics {

    private String instanceId;
    private MetricsEndpoint.MetricResponse metrics;
}
