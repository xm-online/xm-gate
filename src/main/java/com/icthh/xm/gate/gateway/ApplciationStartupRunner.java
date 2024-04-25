package com.icthh.xm.gate.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplciationStartupRunner implements ApplicationRunner {
    private final DiscoveryClient discoveryClient;

    public ApplciationStartupRunner (@Autowired DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        discoveryClient.getServices().forEach(s -> log.info("DISCOVERY REGISTERED SERVICES{}" ,s));
    }

}
