package com.icthh.xm.gate.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.gate.security.oauth2.dto.OAuth2AuthorizationRequest.OAuth2AuthorizationRequestDto;
import jakarta.servlet.http.Cookie;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static com.icthh.xm.gate.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpCookieOAuth2AuthorizationRequestRepositoryUnitTest {

    private HttpCookieOAuth2AuthorizationRequestRepository repository;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        repository = new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Test
    void loadAuthorizationRequestReturnsNullWhenNoCookie() {
        assertNull(repository.loadAuthorizationRequest(request));
    }

    @Test
    void loadAuthorizationRequestReturnsNullWhenCookieInvalid() {
        Cookie cookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, "invalid");
        request.setCookies(cookie);
        assertNull(repository.loadAuthorizationRequest(request));
    }

    // generate test loadAuthorizationRequestReturnsNullWhenCookieValid and contains valid OAuth2AuthorizationRequest
    @Test
    @SneakyThrows
    void loadAuthorizationRequestReturnsNullWhenCookieValid() {
        OAuth2AuthorizationRequest mockRequest = mockRequest();
        OAuth2AuthorizationRequestDto mockRequestDto = mockRequestDto();
        String value = Base64.getEncoder().encodeToString(mockRequestDto.toByteArray());
        Cookie cookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, value);
        request.setCookies(cookie);
        var om = new ObjectMapper();
        assertEquals(om.writeValueAsString(mockRequest), om.writeValueAsString(repository.loadAuthorizationRequest(request)));
    }


    @Test
    void saveAuthorizationRequestAddCookie() {
        OAuth2AuthorizationRequest mockRequest = mockRequest();
        OAuth2AuthorizationRequestDto mockRequestDto = mockRequestDto();
        repository.saveAuthorizationRequest(mockRequest, request, response);

        String exprected = Base64.getEncoder().encodeToString(mockRequestDto.toByteArray());
        String header = response.getHeader("Set-Cookie");
        assertTrue(header.startsWith(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME + "=" + exprected + ";"));
        assertTrue(header.contains("Max-Age=300"));
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("SameSite=Lax"));
        assertFalse(header.contains("Secure"));
    }

    @Test
    void saveAuthorizationRequestAddsSecureOnHttps() {
        request.setSecure(true);

        repository.saveAuthorizationRequest(mockRequest(), request, response);

        assertTrue(response.getHeader("Set-Cookie").contains("Secure"));
    }

    private static OAuth2AuthorizationRequest mockRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("uri")
            .clientId("client")
            .redirectUri("redirect")
            .attributes(Map.of("a", "b"))
            .authorizationRequestUri("auth")
            .additionalParameters(Map.of("c", "d"))
            .scopes(Set.of("scope"))
            .state("state")
            .build();
    }

    private static OAuth2AuthorizationRequestDto mockRequestDto() {
        return OAuth2AuthorizationRequestDto.newBuilder()
            .setAuthorizationUri("uri")
            .setClientId("client")
            .setRedirectUri("redirect")
            .putAttributes("a", "b")
            .setAuthorizationRequestUri("auth")
            .putAdditionalParameters("c", "d")
            .addAllScopes(Set.of("scope"))
            .setState("state")
            .build();
    }

    @Test
    void removeAuthorizationRequestDeletesCookie() {
        OAuth2AuthorizationRequestDto mockRequestDto = mockRequestDto();
        String value = Base64.getEncoder().encodeToString(mockRequestDto.toByteArray());

        Cookie cookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, value);
        request.setCookies(cookie);
        request.setSecure(true);

        repository.removeAuthorizationRequest(request, response);

        String header = response.getHeader("Set-Cookie");
        assertTrue(header.startsWith(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME + "=;"));
        assertTrue(header.contains("Max-Age=0"));
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("SameSite=Lax"));
        assertTrue(header.contains("Secure"));
    }

}
