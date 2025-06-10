package com.icthh.xm.gate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.support.AbstractLoadBalancingClient;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("noconsul")
@Primary
public class SimpleServiceInstanceChooser implements ServiceInstanceChooser {

    private final RibbonLoadBalancerClient delegate;

    @Override
    public ServiceInstance choose(String serviceId) {
        return delegate.choose(serviceId);
    }
}
