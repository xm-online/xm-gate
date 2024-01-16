package com.icthh.xm.gate.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RouteUtils {

    public static String clearRouteId(String routeId) {
        return routeId.substring(routeId.indexOf("_") + 1);
    }
}
