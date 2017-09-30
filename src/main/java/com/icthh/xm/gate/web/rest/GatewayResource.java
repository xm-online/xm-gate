package com.icthh.xm.gate.web.rest;

import com.codahale.metrics.annotation.Timed;
import com.icthh.xm.gate.security.SecurityUtils;
import com.icthh.xm.gate.web.rest.vm.RouteVM;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * REST controller for managing Gateway configuration.
 */
@RestController
@RequestMapping("/api/gateway")
@Slf4j
public class GatewayResource {

    private static final String STATUS = "status";

    private final RouteLocator routeLocator;

    private final DiscoveryClient discoveryClient;

    private final RestTemplate restTemplate = new RestTemplate();

    private final HttpHeaders headers;

    public GatewayResource(RouteLocator routeLocator, DiscoveryClient discoveryClient) {
        this.routeLocator = routeLocator;
        this.discoveryClient = discoveryClient;

        headers = new HttpHeaders();
    }

    /**
     * GET  /routes : get the active routes.
     *
     * @return the ResponseEntity with status 200 (OK) and with body the list of routes
     */
    @GetMapping("/routes")
    @Timed
    public ResponseEntity<List<RouteVM>> activeRoutes() {
        List<Route> routes = routeLocator.getRoutes();
        List<RouteVM> routeVMs = new ArrayList<>();
        routes.forEach(route -> {
            RouteVM routeVM = new RouteVM();
            routeVM.setPath(route.getFullPath());
            routeVM.setServiceId(route.getId());
            List<ServiceInstance> serviceInstances = discoveryClient.getInstances(route.getId());
            routeVM.setServiceInstances(serviceInstances);
            routeVM.setServiceInstancesStatus(receiveServiceStatus(serviceInstances));
            routeVM.setServiceMetadata(extractServiceMetaData(routeVM));
            routeVMs.add(routeVM);
        });
        return new ResponseEntity<>(routeVMs, HttpStatus.OK);
    }

    private Map<String, Object> extractServiceMetaData(RouteVM routeVM) {
        Objects.requireNonNull(routeVM,
            "Can't extract service metadata because routeVM is not pass");

        Map<String, String> serviceInstancesStatus = routeVM.getServiceInstancesStatus();
        if (MapUtils.isEmpty(serviceInstancesStatus)) {
            log.error("Microservice instances has no statuses");
            return null;
        }
        Map<String, Object> result = null;
        for (ServiceInstance instance : routeVM.getServiceInstances()) {
            if (instance.getUri() == null || StringUtils.isBlank(instance.getUri().toString())) {
                continue;
            }
            String uri = instance.getUri().toString();
            if ("UP".equals(serviceInstancesStatus.get(uri))) {
                if (result == null) {
                    result = new HashMap<>();
                }
                result.put(uri, getInstanceInfo(uri));
            }
        }
        return result;
    }

    private Map getInstanceInfo(String uri) {
        headers.clear();
        headers.set("Authorization", "Bearer " + SecurityUtils.extractCurrentToken());
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            Map body = restTemplate.exchange(String.format("%s/management/info", uri),
                HttpMethod.GET, entity, Map.class).getBody();
            return (Map) body.get("build");
        } catch (RestClientException e) {
            log.error("Error occurred while getting metadata of the microservice by URI {}", uri, e);
            return null;
        }
    }

    private Map<String, String> receiveServiceStatus(List<ServiceInstance> instances) {
        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyMap();
        }
        Map<String, String> instancesStatus = new HashMap<>();

        instances.stream()
            .filter(serviceInstance -> serviceInstance.getUri() != null)
            .forEach(instance -> {
                String uri = instance.getUri().toString();
                String status;
                try {
                    Map body = restTemplate.exchange(
                        String.format("%s/management/health", uri),
                        HttpMethod.GET, null, Map.class).getBody();
                    status = (String) body.get(STATUS);
                } catch (RestClientException e) {
                    log.error("Error occurred while getting status of the microservice by URI {}",
                        uri, e);
                    status = "DOWN";
                }
                instancesStatus.put(uri, status);
            });
        return instancesStatus;
    }
}
