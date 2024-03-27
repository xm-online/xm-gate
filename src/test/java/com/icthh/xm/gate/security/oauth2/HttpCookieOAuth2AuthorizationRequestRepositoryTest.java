package com.icthh.xm.gate.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.gate.security.oauth2.dto.OAuth2AuthorizationRequest.OAuth2AuthorizationRequestDto;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import javax.servlet.http.Cookie;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static com.icthh.xm.gate.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    private HttpCookieOAuth2AuthorizationRequestRepository repository;

    @Mock
    private MockHttpServletRequest request;

    @Mock
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        repository = new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Test
    void loadAuthorizationRequestReturnsNullWhenNoCookie() {
        when(request.getCookies()).thenReturn(null);
        assertNull(repository.loadAuthorizationRequest(request));
    }

    @Test
    void loadAuthorizationRequestReturnsNullWhenCookieInvalid() {
        Cookie cookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, "invalid");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
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
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        var om = new ObjectMapper();
        assertEquals(om.writeValueAsString(mockRequest), om.writeValueAsString(repository.loadAuthorizationRequest(request)));
    }


    @Test
    void saveAuthorizationRequestAddCookie() {
        OAuth2AuthorizationRequest mockRequest = mockRequest();
        OAuth2AuthorizationRequestDto mockRequestDto = mockRequestDto();
        repository.saveAuthorizationRequest(mockRequest, request, response);

        String exprected = Base64.getEncoder().encodeToString(mockRequestDto.toByteArray());
        verify(response).addCookie(argThat(it -> it.getMaxAge() > 0 && it.getName().equals(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME) && it.getValue().equals(exprected)));
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
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        repository.removeAuthorizationRequest(request, response);

        verify(response).addCookie(argThat(it -> it.getMaxAge() == 0 && it.getName().equals(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME) && it.getValue().isEmpty()));
    }

}
