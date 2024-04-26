package com.icthh.xm.gate.web.rest;

import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.gate.utils.RouteUtils;
import com.icthh.xm.gate.web.rest.vm.RouteVM;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * REST controller for managing Gateway configuration.
 */
@RestController
@RequestMapping("/api/gateway")
@Slf4j
@RequiredArgsConstructor
public class GatewayResource {

    private static final String STATUS = "status";

    private final RouteLocator routeLocator;

    private final DiscoveryClient discoveryClient;

    private final RestTemplate restTemplate = new RestTemplate();

    private final HttpHeaders headers = new HttpHeaders();

    private final XmAuthenticationContextHolder authContextHolder;

    private static final String[] EXCLUDE_SERVICE = {};

    /**
     * GET  /routes : get the active routes.
     *
     * @return the ResponseEntity with status 200 (OK) and with body the list of routes
     */
    @GetMapping("/routes")
    @Timed
    @PreAuthorize("hasPermission({'returnObject': filterObject, 'log': false}, 'ROUTE.GET_LIST')")
    @PrivilegeDescription("Privilege to get the active routes")
    public Mono<List<RouteVM>> activeRoutes() {
        return routeLocator.getRoutes()
            .filter(r -> routeFilter(RouteUtils.clearRouteId(r.getId())))
            .map(route -> {
                String routeId = RouteUtils.clearRouteId(route.getId());
                RouteVM routeVm = new RouteVM();
                routeVm.setPath(route.getUri().toString());
                routeVm.setServiceId(routeId);
                List<ServiceInstance> serviceInstances = discoveryClient.getInstances(routeId);
                routeVm.setServiceInstances(serviceInstances);
                routeVm.setServiceInstancesStatus(receiveServiceStatus(serviceInstances));
                routeVm.setServiceMetadata(extractServiceMetaData(routeVm));
                return routeVm;
            })
            .collectList();
    }

    private Map<String, Object> extractServiceMetaData(RouteVM routeVM) {
        Objects.requireNonNull(routeVM, "Can't extract service metadata because routeVM is not pass");

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
        XmAuthenticationContext authContext = authContextHolder.getContext();
        Optional<String> tokenValue = authContext.getTokenValue();
        Optional<String> tokenType = authContext.getTokenType();
        if (!tokenValue.isPresent() || !tokenType.isPresent()) {
            throw new IllegalStateException("Authentication not initialized yet, can't create request");
        }
        headers.clear();
        headers.set("Authorization", tokenType.get() + " " + tokenValue.get());
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
                    log.error("Error occurred while getting status of the microservice by URI {}", uri, e);
                    status = "DOWN";
                }
                instancesStatus.put(uri, status);
            });
        return instancesStatus;
    }

    private boolean routeFilter(String routeId) {
        return !ArrayUtils.contains(EXCLUDE_SERVICE, routeId);
    }
}
