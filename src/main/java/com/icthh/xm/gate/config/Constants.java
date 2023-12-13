package com.icthh.xm.gate.config;

import org.springframework.core.Ordered;

/**
 * Application constants.
 */
public final class Constants {

    public static final String HEADER_TENANT = "x-tenant";
    public static final String HEADER_WEBAPP_URL = "x-webapp-url";

    public static final int FILTER_ORDER_TENANT_INIT = Ordered.HIGHEST_PRECEDENCE;
    public static final String DEFAULT_TENANT = "XM";

    private Constants() {}
}
