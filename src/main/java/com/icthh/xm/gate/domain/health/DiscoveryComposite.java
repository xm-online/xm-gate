package com.icthh.xm.gate.domain.health;

import lombok.Data;

@Data
public class DiscoveryComposite {
    private String description;
    private String status;
    private DiscoveryClient discoveryClient;

}
