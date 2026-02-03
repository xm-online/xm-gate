package com.icthh.xm.gate.security.oauth2.idp;

import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tech.jhipster.config.JHipsterProperties;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.icthh.xm.gate.config.Constants.AUTH_RESPONSE_FIELD_IDP_TOKEN;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static com.icthh.xm.gate.security.oauth2.idp.IdpTestUtils.buildAuthentication;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdpAuthenticationSuccessHandlerAbstractUnitTest extends IdpAbstractUnitTest {

    private static final String UAA_TOKEN_URL = "http://uaa/oauth/token";
    private static final String GRANT_TYPE_ATTR = "grant_type";
    private static final String GRANT_TYPE_IDP_TOKEN = "idp_token";
    private static final String TOKEN_ATTR = "token";

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Captor
    private ArgumentCaptor<Consumer<HttpHeaders>> headersCaptor;
    @Captor
    private ArgumentCaptor<MultiValueMap<String, String>> bodyCaptor;

    private JHipsterProperties jhipsterProperties;
    private IdpAuthenticationSuccessHandler idpAuthenticationSuccessHandler;

    @BeforeEach
    void setUp() {
        jhipsterProperties = new JHipsterProperties();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    void shouldThrowException_whenStatefulModeEnabled() {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 1;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true, true);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);
        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        jhipsterProperties.getSecurity().getClientAuthorization().setAccessTokenUri(UAA_TOKEN_URL);

        idpAuthenticationSuccessHandler = new IdpAuthenticationSuccessHandler(
            objectMapper, restClient, tenantContextHolder, idpConfigRepository, jhipsterProperties);

        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> idpAuthenticationSuccessHandler.onAuthenticationSuccess(null, null, null)
        );

        assertEquals("Stateful mode not supported yet", exception.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnTokenData_whenStatelessModeEnabled() throws IOException {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 1;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true, false);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);
        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        jhipsterProperties.getSecurity().getClientAuthorization().setAccessTokenUri(UAA_TOKEN_URL);

        setupRestClientMock();

        idpAuthenticationSuccessHandler = new IdpAuthenticationSuccessHandler(
            objectMapper, restClient, tenantContextHolder, idpConfigRepository, jhipsterProperties);

        MockHttpServletResponse response = new MockHttpServletResponse();
        idpAuthenticationSuccessHandler.onAuthenticationSuccess(null, response, buildAuthentication("email"));

        verifyRestClientInteraction(tenantKey);
        validateResponse(objectMapper.readValue(response.getContentAsString(), Map.class));
    }

    private void setupRestClientMock() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq(UAA_TOKEN_URL))).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(MultiValueMap.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(buildAuthTokenResponse());
    }

    private void verifyRestClientInteraction(String tenantKey) {
        verify(restClient).post();
        verify(requestBodyUriSpec).uri(eq(UAA_TOKEN_URL));
        verify(requestBodySpec).headers(headersCaptor.capture());
        verify(requestBodySpec).body(bodyCaptor.capture());
        verify(requestBodySpec).retrieve();

        validateCapturedHeaders(tenantKey);
        validateCapturedBody();
    }

    private void validateCapturedHeaders(String tenantKey) {
        HttpHeaders headers = new HttpHeaders();
        headersCaptor.getValue().accept(headers);

        assertEquals(MediaType.APPLICATION_FORM_URLENCODED, headers.getContentType());
        assertEquals(tenantKey, headers.getFirst(HEADER_TENANT));
        assertNotNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    private void validateCapturedBody() {
        MultiValueMap<String, String> body = bodyCaptor.getValue();
        assertNotNull(body);
        assertEquals(GRANT_TYPE_IDP_TOKEN, body.getFirst(GRANT_TYPE_ATTR));
        assertEquals("idp.token.value", body.getFirst(TOKEN_ATTR));
    }

    @SuppressWarnings("unchecked")
    private void validateResponse(Map<String, Object> responseValues) {
        assertEquals("idp.token.value", responseValues.get(AUTH_RESPONSE_FIELD_IDP_TOKEN));
        assertEquals("access.token.format", responseValues.get("access_token"));
        assertEquals("XM", responseValues.get("tenant"));
        assertEquals("ROLE_ADMIN", responseValues.get("role_key"));
        assertEquals(43199, responseValues.get("expires_in"));
        assertEquals("25c6844d-0207-44e6-92c9-cb70f5e6d2d7", responseValues.get("jti"));
        assertEquals("openid", responseValues.get("scope"));
        assertEquals("972e08de-5fe3-445c-a81b-507d4e8c8439", responseValues.get("user_key"));
        assertEquals("refresh.token.format", responseValues.get("refresh_token"));

        Map<String, Object> idpAccessTokenInclusion = (Map<String, Object>) responseValues.get("idpAccessTokenInclusion");
        assertNotNull(idpAccessTokenInclusion);
        assertEquals(true, idpAccessTokenInclusion.get("enabled"));
        assertEquals(HttpHeaders.AUTHORIZATION, idpAccessTokenInclusion.get("idpTokenHeader"));
        assertEquals("X-Authorization", idpAccessTokenInclusion.get("xmTokenHeader"));

        List<Map<String, String>> logins = (List<Map<String, String>>) responseValues.get("logins");
        assertNotNull(logins);
        assertEquals(2, logins.size());

        assertLogin(logins.get(0), "login.nickname", "LOGIN.NICKNAME");
        assertLogin(logins.get(1), "login.email", "LOGIN.EMAIL");
    }

    private void assertLogin(Map<String, String> login, String expectedLogin, String expectedTypeKey) {
        assertNotNull(login);
        assertEquals(expectedLogin, login.get("login"));
        assertEquals("", login.get("stateKey"));
        assertEquals(expectedTypeKey, login.get("typeKey"));
    }

    private ResponseEntity<Map<String, Object>> buildAuthTokenResponse() {
        Map<String, Object> body = Map.of(
            "access_token", "access.token.format",
            "token_type", "bearer",
            "refresh_token", "refresh.token.format",
            "expires_in", 43199,
            "scope", "openid",
            "role_key", "ROLE_ADMIN",
            "user_key", "972e08de-5fe3-445c-a81b-507d4e8c8439",
            "tenant", "XM",
            "logins", List.of(
                Map.of("typeKey", "LOGIN.NICKNAME", "stateKey", "", "login", "login.nickname"),
                Map.of("typeKey", "LOGIN.EMAIL", "stateKey", "", "login", "login.email")
            ),
            "jti", "25c6844d-0207-44e6-92c9-cb70f5e6d2d7"
        );
        return ResponseEntity.ok().body(body);
    }
}
