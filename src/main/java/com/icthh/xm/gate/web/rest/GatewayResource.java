package com.icthh.xm.gate.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.gate.service.GatewayServiceInstanceService;
import com.icthh.xm.gate.web.rest.vm.RouteVM;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing Gateway configuration.
 */
@RestController
@RequestMapping("/api/gateway")
@Slf4j
@RequiredArgsConstructor
public class GatewayResource {

    private final GatewayServiceInstanceService gatewayService;

    private final DiscoveryClient discoveryClient;

    /**
     * GET  /routes : get the active routes.
     *
     * @return the ResponseEntity with status 200 (OK) and with body the list of routes
     */
    @GetMapping("/routes")
    @Timed
    @PostFilter("hasPermission({'returnObject': filterObject, 'log': false}, 'ROUTE.GET_LIST')")
    @PrivilegeDescription("Privilege to get the active routes")
    public List<RouteVM> activeRoutes() {
        return discoveryClient.getServices().stream()
            .map(this::buildRouteVM)
            .toList();
    }

    private RouteVM buildRouteVM(String serviceId) {
        String path = "/" + serviceId + "/**";
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);
        Map<String, String> serviceInstancesStatus = gatewayService.receiveServiceStatus(serviceInstances);
        Map<String, Object> serviceMetadata = gatewayService.extractServiceMetaData(serviceInstances, serviceInstancesStatus);

        return new RouteVM(path, serviceId, serviceInstances, serviceInstancesStatus, serviceMetadata);
    }
}
