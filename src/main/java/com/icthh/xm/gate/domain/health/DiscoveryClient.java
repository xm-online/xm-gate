package com.icthh.xm.gate.domain.health;

import lombok.Data;

import java.util.List;

@Data
public class DiscoveryClient {
    private String description;
    private String status;
    private List<String> services = null;

}
