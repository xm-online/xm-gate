package com.icthh.xm.gate.web.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class MsService {

    private String name;
    private List<ServiceInstance> instances = new ArrayList<>();
}
