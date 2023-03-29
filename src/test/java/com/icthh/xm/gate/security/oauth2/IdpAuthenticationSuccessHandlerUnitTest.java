package com.icthh.xm.gate.security.oauth2;

import static com.icthh.xm.gate.config.Constants.AUTH_RESPONSE_FIELD_IDP_TOKEN;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static com.icthh.xm.gate.security.oauth2.IdpAuthenticationSuccessHandler.GRANT_TYPE_ATTR;
import static com.icthh.xm.gate.security.oauth2.IdpAuthenticationSuccessHandler.GRANT_TYPE_IDP_TOKEN;
import static com.icthh.xm.gate.security.oauth2.IdpAuthenticationSuccessHandler.TOKEN_ATTR;
import static com.icthh.xm.gate.security.oauth2.IdpTestUtils.buildAuthentication;
import org.junit.Before;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import io.github.jhipster.config.JHipsterProperties;
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

public class IdpAuthenticationSuccessHandlerUnitTest extends AbstractIdpUnitTest {

    private RestTemplate restTemplate;

    private final JHipsterProperties jhipsterProperties = new JHipsterProperties();

    private IdpAuthenticationSuccessHandler idpAuthenticationSuccessHandler;

    @Before
    public void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();

    }

    @Test
    public void test_shouldFailOnStatefulMode() throws IOException {

        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";

        int clientsAmount = 1;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true, true);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        jhipsterProperties.getSecurity().getClientAuthorization().setAccessTokenUri("http://uaa.com");

        this.idpAuthenticationSuccessHandler =
            new IdpAuthenticationSuccessHandler(objectMapper, restTemplate, tenantContextHolder, idpConfigRepository, jhipsterProperties);

        Exception exception = null;
        try {
            idpAuthenticationSuccessHandler.onAuthenticationSuccess(null, null, null);
        } catch (Exception t) {
            exception = t;
        }

        assertNotNull(exception);
        assertEquals("Stateful mode not supported yet", exception.getMessage());

    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_shouldSuccessfullyReturnData() throws IOException {

        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";

        int clientsAmount = 1;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true, false);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        jhipsterProperties.getSecurity().getClientAuthorization().setAccessTokenUri("http://uaa.com");

        when(restTemplate.exchange(
            eq("http://uaa.com"),
            eq(HttpMethod.POST),
            any(),
            (ParameterizedTypeReference<Map<String, Object>>) any()))
            .thenReturn(buildResponse(true));

        this.idpAuthenticationSuccessHandler =
            new IdpAuthenticationSuccessHandler(objectMapper, restTemplate, tenantContextHolder, idpConfigRepository, jhipsterProperties);

        MockHttpServletResponse response = new MockHttpServletResponse();
        idpAuthenticationSuccessHandler.onAuthenticationSuccess(null, response, buildAuthentication("email"));

        //Capture and validate args
        ArgumentCaptor<Object> uaaTokenRequestCaptor = ArgumentCaptor.forClass(Object.class);

        verify(restTemplate).exchange(
            eq("http://uaa.com"),
            eq(HttpMethod.POST),
            (HttpEntity<?>) uaaTokenRequestCaptor.capture(),
            (ParameterizedTypeReference<Map<String, Object>>) any()
        );

        HttpEntity<MultiValueMap<String, String>> uaaTokenRequestCaptorValues = (HttpEntity<MultiValueMap<String, String>>) uaaTokenRequestCaptor.getValue();

        validateRequestHeaders(uaaTokenRequestCaptorValues, tenantKey);
        validateRequestBody(uaaTokenRequestCaptorValues);

        validateResponse(objectMapper.readValue(response.getContentAsString(), Map.class));
    }

    private void validateRequestHeaders(HttpEntity<MultiValueMap<String, String>> uaaTokenRequestCaptorValues,
                                        String tenantKey) {
        HttpHeaders headers = uaaTokenRequestCaptorValues.getHeaders();
        List<String> xTenantHeader = headers.get(HEADER_TENANT);
        assertNotNull(xTenantHeader);
        assertEquals(1, xTenantHeader.size());
        assertEquals(tenantKey, xTenantHeader.get(0));

        List<String> authorizationHeader = headers.get(HttpHeaders.AUTHORIZATION);
        assertNotNull(authorizationHeader);
        assertEquals(1, authorizationHeader.size());
        assertEquals("Basic bnVsbDpudWxs", authorizationHeader.get(0));

        List<String> contentTypeHeader = headers.get(CONTENT_TYPE);
        assertNotNull(authorizationHeader);
        assertEquals(1, contentTypeHeader.size());
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED_VALUE, contentTypeHeader.get(0));
    }

    private void validateRequestBody(HttpEntity<MultiValueMap<String, String>> uaaTokenRequestCaptorValues) {
        MultiValueMap<String, String> body = uaaTokenRequestCaptorValues.getBody();
        assertNotNull(body);

        List<String> grantType = body.get(GRANT_TYPE_ATTR);
        assertNotNull(grantType);
        assertEquals(1, grantType.size());
        assertEquals(GRANT_TYPE_IDP_TOKEN, grantType.get(0));

        assertNotNull(body.get(TOKEN_ATTR));
        assertEquals("idp.token.value", body.get(TOKEN_ATTR).get(0));
    }

    private void validateResponse(Map<String, Object> responseValues) throws JsonProcessingException, UnsupportedEncodingException {

        assertEquals("idp.token.value", String.valueOf(responseValues.get(AUTH_RESPONSE_FIELD_IDP_TOKEN)));
        assertEquals("access.token.format", String.valueOf(responseValues.get("access_token")));
        assertEquals("XM", String.valueOf(responseValues.get("tenant")));
        assertEquals("ROLE_ADMIN", String.valueOf(responseValues.get("role_key")));
        assertEquals(43199, responseValues.get("expires_in"));
        assertEquals("25c6844d-0207-44e6-92c9-cb70f5e6d2d7", String.valueOf(responseValues.get("jti")));
        assertEquals("openid", String.valueOf(responseValues.get("scope")));
        assertEquals("972e08de-5fe3-445c-a81b-507d4e8c8439", String.valueOf(responseValues.get("user_key")));
        assertEquals("refresh.token.format", String.valueOf(responseValues.get("refresh_token")));

        Map<String, Object> idpAccessTokenInclusion = (Map<String, Object>) responseValues.get("idpAccessTokenInclusion");
        assertNotNull(idpAccessTokenInclusion);
        assertEquals(true, idpAccessTokenInclusion.get("enabled"));
        assertEquals(HttpHeaders.AUTHORIZATION, idpAccessTokenInclusion.get("idpTokenHeader"));
        assertEquals("X-Authorization", idpAccessTokenInclusion.get("xmTokenHeader"));

        List<Map<String, String>> logins = (List<Map<String, String>>) responseValues.get("logins");
        assertNotNull(logins);
        assertEquals(2, logins.size());
        Map<String, String> firstLogin = logins.get(0);
        assertNotNull(firstLogin);
        assertEquals("login.nickname", firstLogin.get("login"));
        assertEquals("", firstLogin.get("stateKey"));
        assertEquals("LOGIN.NICKNAME", firstLogin.get("typeKey"));

        Map<String, String> secondLogin = logins.get(1);
        assertNotNull(secondLogin);
        assertEquals("login.email", secondLogin.get("login"));
        assertEquals("", secondLogin.get("stateKey"));
        assertEquals("LOGIN.EMAIL", secondLogin.get("typeKey"));
    }

    private ResponseEntity<Map<String, Object>> buildResponse(boolean buildValidResponse) {
        if (!buildValidResponse) {
            new ResponseEntity<Map<String, Object>>(null, null, HttpStatus.BAD_REQUEST);
        }

        HttpStatus status = HttpStatus.OK;
        Map<String, Object> body = Map.of(
            "access_token", "access.token.format",
            "token_type", OAuth2AccessToken.BEARER_TYPE.toLowerCase(),
            "refresh_token", "refresh.token.format",
            "expires_in", 43199,
            "scope", "openid",
            "role_key", "ROLE_ADMIN",
            "user_key", "972e08de-5fe3-445c-a81b-507d4e8c8439",
            "tenant", "XM",
            "logins", List.of(
                Map.of("typeKey", "LOGIN.NICKNAME",
                    "stateKey", "",
                    "login", "login.nickname"),
                Map.of("typeKey", "LOGIN.EMAIL",
                    "stateKey", "",
                    "login", "login.email")
            ),
            "jti", "25c6844d-0207-44e6-92c9-cb70f5e6d2d7"
        );
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

        return new ResponseEntity<>(body, headers, status);
    }

}
