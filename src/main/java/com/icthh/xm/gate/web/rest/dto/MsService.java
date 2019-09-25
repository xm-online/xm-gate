package com.icthh.xm.gate.web.rest.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MsService {

    private String name;
    @Builder.Default
    private List<ServiceInstance> instances = new ArrayList<>();
}
