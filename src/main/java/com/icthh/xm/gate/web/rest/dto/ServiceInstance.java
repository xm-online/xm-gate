package com.icthh.xm.gate.web.rest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceInstance {

    private String id;
    private String address;
    private Integer port;
}
