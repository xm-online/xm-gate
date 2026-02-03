package com.icthh.xm.gate.web.rest.vm;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.Map;

/**
 * View Model that stores a route managed by the Gateway.
 */
public record RouteVM(
    String path,
    String serviceId,
    List<ServiceInstance> serviceInstances,
    Map<String, String> serviceInstancesStatus,
    Map<String, Object> serviceMetadata
) {}