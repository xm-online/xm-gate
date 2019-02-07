package com.icthh.xm.gate.domain;

public class HealthResponse {

    public String description;
    public String status;
    public DiscoveryComposite discoveryComposite;
    public DiskSpace diskSpace;
    public Db db;
    public RefreshScope refreshScope;
    public Hystrix hystrix;
    public Consul consul;

}
