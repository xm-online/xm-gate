package com.icthh.xm.gate.web.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceHealth {

    private String instanceId;
    private String rawOutput;
    private String status;
}
