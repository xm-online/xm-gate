package com.icthh.xm.gate.domain.health;

import lombok.Data;

import java.util.List;

@Data
public class Services {
    private List<String> config;
    private List<Object> consul;
    private List<String> entity;
    private List<String> gate;
    private List<String> uaa;
}
