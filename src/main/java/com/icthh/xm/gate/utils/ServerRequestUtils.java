package com.icthh.xm.gate.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.CLIENT_ID;

@UtilityClass
public class ServerRequestUtils {

    public static String getClientIdFromToken(String jwtToken) {
        if (jwtToken == null || !jwtToken.startsWith("Bearer ")) {
            return StringUtils.EMPTY;
        }
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder().setSkipSignatureVerification().build();
            return jwtConsumer.processToClaims(jwtToken.replace("Bearer ", "")).getClaimValueAsString(CLIENT_ID);
        } catch (InvalidJwtException e) {
            throw new RuntimeException(e);
        }
    }
}
