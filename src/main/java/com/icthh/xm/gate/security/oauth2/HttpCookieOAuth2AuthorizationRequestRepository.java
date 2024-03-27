package com.icthh.xm.gate.security.oauth2;

import com.google.protobuf.InvalidProtocolBufferException;
import com.icthh.xm.gate.security.oauth2.dto.OAuth2AuthorizationRequest.OAuth2AuthorizationRequestDto;
import com.icthh.xm.gate.utils.oauth2.CookieUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

@Slf4j
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_request";
    private static final int cookieExpireSeconds = 300;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> deserialize(cookie, request))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            return;
        }

        CookieUtils.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialize(authorizationRequest), cookieExpireSeconds);
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        Map<String, String> additionalParameters = convertMapTo(authorizationRequest.getAdditionalParameters());
        Map<String, String> attributes = convertMapTo(authorizationRequest.getAttributes());

        byte[] serialized = OAuth2AuthorizationRequestDto.newBuilder()
            .setAuthorizationUri(authorizationRequest.getAuthorizationUri())
            .setClientId(authorizationRequest.getClientId())
            .setRedirectUri(authorizationRequest.getRedirectUri())
            .addAllScopes(authorizationRequest.getScopes())
            .setState(authorizationRequest.getState())
            .putAllAttributes(attributes)
            .putAllAdditionalParameters(additionalParameters)
            .setAuthorizationRequestUri(authorizationRequest.getAuthorizationRequestUri())
            .build()
            .toByteArray();
        return Base64.getEncoder().encodeToString(serialized);
    }

    private static Map<String, String> convertMapTo(Map<String, Object> input) {
        Map<String, String> result = new HashMap<>();
        input = firstNonNull(input, emptyMap());
        input.forEach((key, value) -> result.put(key, String.valueOf(value)));
        return result;
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest oAuth2AuthorizationRequest = loadAuthorizationRequest(request);
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        return oAuth2AuthorizationRequest;
    }

    private OAuth2AuthorizationRequest deserialize(Cookie cookie, HttpServletRequest request) {
        try {
            OAuth2AuthorizationRequestDto dto = OAuth2AuthorizationRequestDto.parseFrom(Base64.getDecoder().decode(cookie.getValue()));
            List<String> scopesList = firstNonNull(dto.getScopesList(), emptyList());
            Map<String, Object> additionalParameters = new HashMap<>(
                firstNonNull(dto.getAdditionalParametersMap(), emptyMap())
            );
            Map<String, Object> attributes = new HashMap<>(
                firstNonNull(dto.getAttributesMap(), emptyMap())
            );

            return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(dto.getAuthorizationUri())
                .clientId(dto.getClientId())
                .redirectUri(dto.getRedirectUri())
                .scopes(new HashSet<>(scopesList))
                .state(dto.getState())
                .additionalParameters(additionalParameters)
                .attributes(attributes)
                .authorizationRequestUri(dto.getAuthorizationRequestUri())
                .build();
        } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            log.error("Failed to deserialize OAuth2AuthorizationRequestDto from cookie", e);
            return null;
        }
    }
}
