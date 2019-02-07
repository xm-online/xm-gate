package com.icthh.xm.gate.domain.health;

import lombok.Data;

@Data
public class HealthResponse {
    private String description;
    private String status;
    private DiscoveryComposite discoveryComposite;
    private DiskSpace diskSpace;
    private Db db;
    private RefreshScope refreshScope;
    private Hystrix hystrix;
    private Consul consul;

}
