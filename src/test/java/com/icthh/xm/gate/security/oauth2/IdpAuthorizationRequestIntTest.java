package com.icthh.xm.gate.security.oauth2;

import com.icthh.xm.gate.IntegrationTest;
import com.icthh.xm.gate.security.oauth2.idp.IdpClientRepository;
import com.icthh.xm.gate.service.TenantMappingService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

import static com.icthh.xm.gate.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The IdP redirect through the real security chain.
 */
@IntegrationTest
@AutoConfigureMockMvc
class IdpAuthorizationRequestIntTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private IdpClientRepository idpClientRepository;
    @Autowired
    private TenantMappingService tenantMappingService;

    @BeforeEach
    void registerIdp() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("idp")
            .clientId("client")
            .clientSecret("secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/login/oauth2/code/idp")
            .scope("openid")
            .authorizationUri("http://idp.test/authorize")
            .tokenUri("http://idp.test/token")
            .jwkSetUri("http://idp.test/jwks")
            .build();
        idpClientRepository.setRegistrations(tenantMappingService.getTenantKey("localhost"), List.of(registration));
    }

    @Test
    void redirectCarriesChallengeOfTheStoredVerifier() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/oauth2/authorization/idp")).andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(302);
        List<String> cookies = response.getHeaders("Set-Cookie");
        assertThat(cookies).hasSize(1);
        assertThat(cookies.get(0)).startsWith(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME + "=")
            .contains("HttpOnly", "SameSite=Lax");

        var query = UriComponentsBuilder.fromUriString(response.getRedirectedUrl()).build().getQueryParams();
        OAuth2AuthorizationRequest stored = storedRequest(cookies.get(0));
        assertThat(query.getFirst("code_challenge_method")).isEqualTo("S256");
        assertThat(query.getFirst("code_challenge")).isEqualTo(s256(stored.getAttribute("code_verifier")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"code_verifier", "code_challenge", "code_challenge_method"})
    void reservedParameterIsRejected(String name) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(get("/oauth2/authorization/idp").param(name, "x"))
            .andReturn().getResponse();

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    }

    private static OAuth2AuthorizationRequest storedRequest(String setCookie) {
        String value = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        return new OAuth2AuthorizationRequestCodec().decode(Base64.getDecoder().decode(value));
    }

    private static String s256(String verifier) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
