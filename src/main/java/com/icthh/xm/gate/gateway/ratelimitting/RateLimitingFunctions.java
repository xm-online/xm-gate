package com.icthh.xm.gate.gateway.ratelimitting;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cloud.gateway.server.mvc.filter.Bucket4jFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.util.Optional;

import static com.icthh.xm.gate.config.Constants.DEFAULT_TENANT;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static com.icthh.xm.gate.gateway.ratelimitting.ServerRequestUtils.SESSION_ID_HEADER;
import static com.icthh.xm.gate.gateway.ratelimitting.ServerRequestUtils.extractServiceName;
import static com.icthh.xm.gate.gateway.ratelimitting.ServerRequestUtils.getClientIdFromToken;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class RateLimitingFunctions {

    public static HandlerFilterFunction<ServerResponse, ServerResponse> rateLimitByTenantKey(int capacity, int periodInMinutes) {
        return Bucket4jFilterFunctions.rateLimit(c -> c
            .setCapacity(capacity)
            .setPeriod(Duration.ofMinutes(periodInMinutes))
            .setKeyResolver(RateLimitingFunctions::getTenantKey)
        );
    }

    public static HandlerFilterFunction<ServerResponse, ServerResponse> rateLimitByClientKey(int capacity, int periodInMinutes) {
        return Bucket4jFilterFunctions.rateLimit(c -> c
            .setCapacity(capacity)
            .setPeriod(Duration.ofMinutes(periodInMinutes))
            .setKeyResolver(RateLimitingFunctions::getClientIdKey)
        );
    }

    public static HandlerFilterFunction<ServerResponse, ServerResponse> rateLimitBySessionKey(int capacity, int periodInMinutes) {
        return Bucket4jFilterFunctions.rateLimit(c -> c
            .setCapacity(capacity)
            .setPeriod(Duration.ofMinutes(periodInMinutes))
            .setKeyResolver(RateLimitingFunctions::getSessionIdKey)
        );
    }

    private static String getSessionIdKey(ServerRequest serverRequest) {
        HttpServletRequest request = serverRequest.servletRequest();
        String sessionId = request.getHeader(SESSION_ID_HEADER);
        String serviceName = extractServiceName(request.getRequestURI());
        // if sessionId is blank, return EMPTY string to disable rate limiting by this key
        return isNotBlank(sessionId) ? serviceName + ":" + sessionId : EMPTY;
    }

    private static String getClientIdKey(ServerRequest serverRequest) {
        HttpServletRequest request = serverRequest.servletRequest();
        String tenantKey = getTenantKey(serverRequest);
        String clientId = getClientIdFromToken(request);
        return isBlank(clientId) ? tenantKey : tenantKey + ":" + clientId;
    }

    private static String getTenantKey(ServerRequest serverRequest) {
        HttpServletRequest request = serverRequest.servletRequest();
        return Optional.ofNullable(request.getHeader(HEADER_TENANT)).orElse(DEFAULT_TENANT);
    }

    public static class FilterSupplier extends SimpleFilterSupplier {
        public FilterSupplier() {
            super(RateLimitingFunctions.class);
        }
    }
}
