package com.icthh.xm.gate.domain.health;

import lombok.Data;

@Data
public class Consul {
    private String status;
    private Services services;
    private String advertiseAddress;
    private String datacenter;
    private String domain;
    private String nodeName;
    private String bindAddress;
    private String clientAddress;

}

