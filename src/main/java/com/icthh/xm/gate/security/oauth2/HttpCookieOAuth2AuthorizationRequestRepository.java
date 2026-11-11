package com.icthh.xm.gate.security.oauth2;

import com.icthh.xm.gate.utils.oauth2.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Base64;

@Slf4j
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_request";
    private static final int cookieExpireSeconds = 300;

    private final OAuth2AuthorizationRequestCodec codec;

    public HttpCookieOAuth2AuthorizationRequestRepository() {
        this(new OAuth2AuthorizationRequestCodec());
    }

    public HttpCookieOAuth2AuthorizationRequestRepository(OAuth2AuthorizationRequestCodec codec) {
        this.codec = codec;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return CookieUtils.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(this::deserialize)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            return;
        }

        String value = Base64.getEncoder().encodeToString(codec.encode(authorizationRequest));
        CookieUtils.addCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, value, cookieExpireSeconds);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest oAuth2AuthorizationRequest = loadAuthorizationRequest(request);
        CookieUtils.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        return oAuth2AuthorizationRequest;
    }

    private OAuth2AuthorizationRequest deserialize(Cookie cookie) {
        try {
            return codec.decode(Base64.getDecoder().decode(cookie.getValue()));
        } catch (IllegalArgumentException e) {
            log.error("Failed to deserialize OAuth2AuthorizationRequestDto from cookie", e);
            return null;
        }
    }
}
