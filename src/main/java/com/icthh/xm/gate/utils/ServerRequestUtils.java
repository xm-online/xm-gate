package com.icthh.xm.gate.utils;

import lombok.experimental.UtilityClass;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.CLIENT_ID;

@UtilityClass
public class ServerRequestUtils {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String BEARER_UPPER_PREFIX = "BEARER ";
    private static final String EMPTY = " ";

    public static String getClientIdFromToken(String jwtToken) {
        if (jwtToken == null || !jwtToken.startsWith(BEARER_PREFIX)) {
            return EMPTY;
        }
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder().setSkipSignatureVerification().build();
            JwtClaims claims = jwtConsumer.processToClaims(jwtToken.replace(BEARER_PREFIX, EMPTY));
            return claims.getClaimValueAsString(CLIENT_ID);

        } catch (InvalidJwtException e) {
            throw new RuntimeException(e);
        }
    }

    public static String resolveTokenFromRequest(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION);
        if (StringUtils.hasText(bearerToken)
            && (bearerToken.startsWith(BEARER_PREFIX) || bearerToken.toUpperCase().startsWith(BEARER_UPPER_PREFIX))) {

            String token = bearerToken.substring(7);
            return StringUtils.hasText(token) ? token.strip() : token;

        } else {
            return null;
        }
    }
}
