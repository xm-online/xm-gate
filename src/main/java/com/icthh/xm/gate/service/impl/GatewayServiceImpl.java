package com.icthh.xm.gate.service.impl;

import com.icthh.xm.gate.service.GatewayService;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class GatewayServiceImpl implements GatewayService {

    private final RouteLocator routeLocator;

    public GatewayServiceImpl(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @Override
    public Set<String> getRegisteredServiceIds() {
        Set<String> registeredServiceIds = new HashSet<>();

        routeLocator.getRoutes().subscribe(route -> {
            registeredServiceIds.add(route.getId().substring(route.getId().indexOf("_") + 1).toLowerCase());
        });
        return  registeredServiceIds;
    }
}
