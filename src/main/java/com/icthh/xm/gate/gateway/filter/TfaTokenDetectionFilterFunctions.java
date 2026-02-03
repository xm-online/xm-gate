package com.icthh.xm.gate.gateway.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.icthh.xm.commons.security.XmAuthenticationConstants.AUTH_ADDITIONAL_DETAILS;
import static java.lang.Boolean.FALSE;

/**
 * HandlerFilterFunction for detecting TFA (Two-Factor Authentication) tokens
 * and blocking their inappropriate usage.
 */
@Slf4j
public class TfaTokenDetectionFilterFunctions {

    private static final String TFA_VERIFICATION_KEY_CLAIM = "tfaVerificationKey";
    private static final Set<String> SKIP_FILTERING_PATH = Set.of("/uaa/oauth/token");

    private static final Base64.Decoder decoder = Base64.getUrlDecoder();
    private static final BearerTokenResolver tokenResolver = new DefaultBearerTokenResolver();

    public static HandlerFilterFunction<ServerResponse, ServerResponse> tfaTokenDetection() {
        return (request, next) -> {
            HttpServletRequest servletRequest = request.servletRequest();

            if (shouldNotFilter(servletRequest)) {
                log.debug("Skip tfa token detection filter");
                return next.handle(request);
            }

            JwtDecoder jwtDecoder = MvcUtils.getApplicationContext(request)
                .getBean(JwtDecoder.class);

            try {
                if (isTfaTokenUsedInappropriately(servletRequest, jwtDecoder)) {
                    log.warn("Forbidden, inappropriate TFA access token usage: {}", servletRequest.getRequestURI());
                    return ServerResponse.status(HttpStatus.FORBIDDEN)
                        .body("Forbidden, inappropriate TFA access token use");
                }
            } catch (Exception e) {
                log.error("Internal error during token validation", e);
                return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal error during token validation");
            }

            return next.handle(request);
        };
    }

    private static boolean shouldNotFilter(HttpServletRequest request) {
        return SKIP_FILTERING_PATH.contains(request.getRequestURI()) || !isTfaAccessToken(request);
    }

    private static Optional<String> extractBearerToken(HttpServletRequest request) {
        return Optional.ofNullable(tokenResolver.resolve(request));
    }

    private static boolean isTfaTokenUsedInappropriately(HttpServletRequest request, JwtDecoder jwtDecoder) {
        return extractBearerToken(request)
            .map(jwtDecoder::decode)
            .map(TfaTokenDetectionFilterFunctions::hasTfaVerificationKeyClaim)
            .orElse(FALSE);
    }

    private static boolean hasTfaVerificationKeyClaim(Jwt jwt) {
        try {
            Map<String, Object> details = jwt.getClaim(AUTH_ADDITIONAL_DETAILS);
            return details != null && !details.isEmpty() && details.containsKey(TFA_VERIFICATION_KEY_CLAIM);

        } catch (ClassCastException e) {
            log.error("Failed to cast token.additionalDetails claim to map");
            return false;
        }
    }

    private static boolean isTfaAccessToken(HttpServletRequest request) {
        return extractBearerToken(request)
            .map(token -> token.split("\\.")[1])
            .map(decoder::decode)
            .map(decoded -> new String(decoded, StandardCharsets.UTF_8))
            .map(decoded -> decoded.toLowerCase().contains("tfa"))
            .orElse(false);
    }

    public static class FilterSupplier extends SimpleFilterSupplier {
        public FilterSupplier() {
            super(TfaTokenDetectionFilterFunctions.class);
        }
    }
}
