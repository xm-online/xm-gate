package com.icthh.xm.gate.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.lang.reflect.Field;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.CLIENT_ID;

@Slf4j
@UtilityClass
public class ServerRequestUtils {

    public static final String SESSION_ID_HEADER = "X-SESSIONID";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BEARER_UPPER_PREFIX = "BEARER ";
    private static final String MUTATED_REQUEST_CLASS_NAME = "MutatedServerHttpRequest";
    private static final String ORIGINAL_REQUEST_FIELD_NAME = "originalRequest";

    public static String getClientIdFromToken(String jwtToken) {
        if (jwtToken == null || !jwtToken.startsWith(BEARER_PREFIX)) {
            return StringUtils.EMPTY;
        }
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder().setSkipSignatureVerification().build();
            JwtClaims claims = jwtConsumer.processToClaims(jwtToken.replace(BEARER_PREFIX, StringUtils.EMPTY));
            return claims.getClaimValueAsString(CLIENT_ID);

        } catch (InvalidJwtException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getServiceNameFromRequestPath(ServerHttpRequest request) {
        var uriElements = request.getPath().elements();

        // ServerHttpRequest can mutate and lost the first path element containing service name. It's necessary
        // to get originalRequest to obtain a full path from it
        ServerHttpRequest originalRequest = request;
        while (isMutatedRequest(originalRequest)) {
            originalRequest = getOriginalRequest(originalRequest);
            uriElements = originalRequest.getPath().elements();
        }
        return uriElements.size() < 2 ? StringUtils.EMPTY : uriElements.get(1).value();
    }

    public static String resolveTokenFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION);
        if (StringUtils.isNotBlank(bearerToken)
            && (bearerToken.startsWith(BEARER_PREFIX) || bearerToken.toUpperCase().startsWith(BEARER_UPPER_PREFIX))) {

            String token = bearerToken.substring(7);
            return StringUtils.isNotBlank(token) ? token.strip() : token;

        } else {
            return null;
        }
    }

    private static ServerHttpRequest getOriginalRequest(ServerHttpRequest request) {
        try {
            Field originalRequestField = request.getClass().getDeclaredField(ORIGINAL_REQUEST_FIELD_NAME);
            originalRequestField.setAccessible(true);
            return  (ServerHttpRequest) originalRequestField.get(request);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Exception occurred while receiving original request path with message: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static boolean isMutatedRequest(ServerHttpRequest request) {
        return request.getClass().getName().endsWith(MUTATED_REQUEST_CLASS_NAME);
    }
}
