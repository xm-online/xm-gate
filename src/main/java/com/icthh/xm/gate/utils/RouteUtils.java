package com.icthh.xm.gate.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RouteUtils {

    public static String clearRouteId(String routeId) {
        // ReactiveCompositeDiscoveryClient adds prefix to the route IDs to avoid naming conflicts and clearly indicate their origin
        return routeId.substring(routeId.indexOf("_") + 1);
    }
}
