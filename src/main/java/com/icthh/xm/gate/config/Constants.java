package com.icthh.xm.gate.config;

import org.springframework.core.Ordered;

/**
 * Application constants.
 */
public final class Constants {

    public static final String HEADER_TENANT = "x-tenant";
    public static final String HEADER_WEBAPP_URL = "x-webapp-url";

    public static final int FILTER_ORDER_TENANT_INIT = Ordered.HIGHEST_PRECEDENCE;
    public static final int FILTER_DOMAIN_RELAY_ORDER = Ordered.HIGHEST_PRECEDENCE + 1;
    public static final String DEFAULT_TENANT = "XM";

    // rate limiting configuration constants
    public static final String FILTER_NAME = "RequestRateLimiter";
    public static final String REPLENISH_RATE = "redis-rate-limiter.replenishRate";
    public static final String BURST_CAPACITY = "redis-rate-limiter.burstCapacity";
    public static final String REQUESTED_TOKENS = "redis-rate-limiter.requestedTokens";
    public static final String KEY_RESOLVER = "key-resolver";
    public static final String DENY_EMPTY = "deny-empty-key";

    private Constants() {}
}
