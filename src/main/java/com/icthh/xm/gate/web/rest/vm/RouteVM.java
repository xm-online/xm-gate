package com.icthh.xm.gate.web.rest.vm;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.Map;

/**
 * View Model that stores a route managed by the Gateway.
 */
public class RouteVM {

    private String path;

    private String serviceId;

    private List<ServiceInstance> serviceInstances;

    private Map<String, String> serviceInstancesStatus;

    private Map<String, Object> serviceMetadata;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public List<ServiceInstance> getServiceInstances() {
        return serviceInstances;
    }

    public void setServiceInstances(List<ServiceInstance> serviceInstances) {
        this.serviceInstances = serviceInstances;
    }

    public Map<String, String> getServiceInstancesStatus() {
        return serviceInstancesStatus;
    }

    public void setServiceInstancesStatus(Map<String, String> serviceInstancesStatus) {
        this.serviceInstancesStatus = serviceInstancesStatus;
    }

    public Map<String, Object> getServiceMetadata() {
        return serviceMetadata;
    }

    public void setServiceMetadata(Map<String, Object> serviceMetadata) {
        this.serviceMetadata = serviceMetadata;
    }
}
