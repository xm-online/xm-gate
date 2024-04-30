package com.icthh.xm.gate.web.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.gate.security.SecurityUtils;
import com.icthh.xm.gate.utils.RouteUtils;
import com.icthh.xm.gate.web.rest.vm.RouteVM;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * REST controller for managing Gateway configuration.
 */
@RestController
@RequestMapping("/api/gateway")
@Slf4j
@RequiredArgsConstructor
public class GatewayResource {

    private static final String STATUS_FIELD = "status";
    private static final String BUILD_FIELD = "build";
    private static final String UP_STATUS = "UP";
    private static final String DOWN_STATUS = "DOWN";

    private final RouteLocator routeLocator;

    private final DiscoveryClient discoveryClient;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

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
        Mono<String> tokenTypeMono = SecurityUtils.getTokenTypeOrError();
        Mono<String> tokenValueMono = SecurityUtils.getTokenValueOrError();

        return routeLocator.getRoutes()
            .filter(r -> routeFilter(RouteUtils.clearRouteId(r.getId())))
            .map(this::buildRouteVm)
            .flatMap(r -> Mono.zip(Mono.just(r), receiveServiceStatus(r.getServiceInstances())))
            .map(data -> {
                data.getT1().setServiceInstancesStatus(data.getT2());
                return data.getT1();
            })
            .flatMap(r -> Mono.zip(Mono.just(r), extractServiceMetaData(r, tokenTypeMono, tokenValueMono)))
            .map(data -> {
                data.getT1().setServiceMetadata(data.getT2());
                return data.getT1();
            })
            .collectList();
    }

    private RouteVM buildRouteVm(Route route) {
        String routeId = RouteUtils.clearRouteId(route.getId());
        RouteVM routeVm = new RouteVM();
        routeVm.setPath(route.getUri().toString());
        routeVm.setServiceId(routeId);
        routeVm.setServiceInstances(discoveryClient.getInstances(routeId));
        return routeVm;
    }

    private Mono<Map<String, Object>> extractServiceMetaData(RouteVM routeVM, Mono<String> tokenTypeMono, Mono<String> tokenValueMono) {
        Objects.requireNonNull(routeVM, "Can't extract service metadata because routeVM is not pass");

        Map<String, String> serviceInstancesStatus = routeVM.getServiceInstancesStatus();
        if (MapUtils.isEmpty(serviceInstancesStatus)) {
            log.error("Microservice instances has no statuses");
            return Mono.just(Map.of());
        }
        return Flux.fromIterable(routeVM.getServiceInstances())
            .filter(i -> i.getUri() != null && StringUtils.isNotBlank(i.getUri().toString()))
            .map(i -> i.getUri().toString())
            .filter(uri -> UP_STATUS.equals(serviceInstancesStatus.get(uri)))
            .flatMap(uri -> Mono.zip(Mono.just(uri), tokenTypeMono, tokenValueMono))
            .flatMap(data -> Mono.zip(Mono.just(data.getT1()), getInstanceInfo(data.getT1(), data.getT2(), data.getT3())))
            .map(data -> new AbstractMap.SimpleEntry<>(data.getT1(), data.getT2()))
            .collectMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue);
    }

    private Mono<Map<String, Object>> getInstanceInfo(String uri, String tokenType, String tokenValue) {
        if (StringUtils.isBlank(tokenValue) || StringUtils.isBlank(tokenType)) {
            throw new IllegalStateException("Authentication not initialized yet, can't create request");
        }
        return webClient.get()
            .uri(String.format("%s/management/info", uri))
            .header("Authorization", tokenType + " " + tokenValue)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> objectMapper.convertValue(response.get(BUILD_FIELD), new TypeReference<Map<String, Object>>() {
            }))
            .onErrorResume(e -> {
                log.error("Error occurred while getting metadata of the microservice by URI {}", uri, e);
                return Mono.empty();
            });
    }

    private Mono<Map<String, String>> receiveServiceStatus(List<ServiceInstance> instances) {
        return Flux.fromIterable(instances)
            .filter(serviceInstance -> serviceInstance.getUri() != null)
            .flatMap(i -> Mono.zip(Mono.just(i), receiveServiceStatus(i)))
            .map(data -> new AbstractMap.SimpleEntry<>(data.getT1().getUri().toString(), data.getT2()))
            .collectMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)
            .defaultIfEmpty(Map.of());
    }

    private Mono<String> receiveServiceStatus(ServiceInstance serviceInstance) {
        if (serviceInstance == null || serviceInstance.getUri() == null) {
            return Mono.empty();
        }
        String uri = serviceInstance.getUri().toString();

        return webClient.get()
            .uri(String.format("%s/management/health", uri))
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> response.get(STATUS_FIELD).toString())
            .onErrorResume(e -> {
                log.error("Error occurred while getting status of the microservice by URI {}", uri, e);
                return Mono.just(DOWN_STATUS);
            });
    }

    private boolean routeFilter(String routeId) {
        return !ArrayUtils.contains(EXCLUDE_SERVICE, routeId);
    }
}
