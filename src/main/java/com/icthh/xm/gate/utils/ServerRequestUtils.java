package com.icthh.xm.gate.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import static org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames.CLIENT_ID;

@Slf4j
@UtilityClass
public class ServerRequestUtils {

    public static final String BEARER_PREFIX = "Bearer ";

    private static final BearerTokenResolver tokenResolver = new DefaultBearerTokenResolver();

    public static JwtClaims getJwtTokenClaims(HttpServletRequest request) {
        String jwtToken = tokenResolver.resolve(request);
        if (jwtToken == null) {
            return new JwtClaims();
        }
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setSkipSignatureVerification()
                .setSkipDefaultAudienceValidation()
                .build();
            return jwtConsumer.processToClaims(jwtToken.replace(BEARER_PREFIX, StringUtils.EMPTY));

        } catch (InvalidJwtException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getClientIdFromToken(HttpServletRequest request) {
        JwtClaims claims = getJwtTokenClaims(request);
        return claims.getClaimValueAsString(CLIENT_ID);
    }

    /**
     * Remove the optional api prefix (see {@code application.gateway.api-prefix}) from a request URI.
     * The prefix is optional, so a URI that does not carry it is returned as is - both forms stay usable.
     * Examples with the /xm-api prefix:
     * <pre>
     *   /xm-api/uaa/oauth/token -> /uaa/oauth/token
     *   /xm-api                 -> /
     *   /uaa/oauth/token        -> /uaa/oauth/token  (no prefix, returned as is)
     * </pre>
     */
    public static String stripApiPrefix(String requestUri, String apiPrefix) {
        String prefix = normalizeApiPrefix(apiPrefix);
        if (requestUri == null || prefix == null) {
            return requestUri;
        }
        if (requestUri.equals(prefix)) {
            return "/";
        }
        if (requestUri.startsWith(prefix + "/")) {
            return requestUri.substring(prefix.length());
        }
        return requestUri;
    }

    /**
     * Bring the configured api prefix to a canonical path: no surrounding blanks, one leading slash,
     * no trailing slash.
     * <pre>
     *   "  /xm-api  " -> "/xm-api"
     *   "xm-api"      -> "/xm-api"
     *   "/xm-api/"    -> "/xm-api"
     *   "", "/", null -> null       (not configured, the prefixed form is off)
     * </pre>
     */
    public static String normalizeApiPrefix(String apiPrefix) {
        if (apiPrefix == null) {
            return null;
        }

        String prefix = apiPrefix.trim();
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if (prefix.isEmpty()) {
            return null;
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        return prefix;
    }

    /**
     * Extract service name from request URI.
     * Example: /serviceName/api/smth -> serviceName
     */
    public static String extractServiceName(String requestUri) {
        String path = requestUri;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        int slashIndex = path.indexOf('/');
        if (slashIndex > 0) {
            return path.substring(0, slashIndex);
        }
        return path.isEmpty() ? null : path;
    }

    /**
     * Extract path within service from request URI.
     * Example: /serviceName/api/smth -> /api/smth
     */
    public static String extractPathWithinService(String requestUri, String serviceName) {
        String prefix = "/" + serviceName;
        if (requestUri.startsWith(prefix)) {
            String pathWithinService = requestUri.substring(prefix.length());
            return pathWithinService.isEmpty() ? "/" : pathWithinService;
        }
        return requestUri;
    }
}
