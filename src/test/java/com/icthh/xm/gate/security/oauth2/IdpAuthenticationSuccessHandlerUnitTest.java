package com.icthh.xm.gate.security.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.gate.domain.idp.IdpConfigContainer;
import io.github.jhipster.config.JHipsterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_PRIVATE_SETTINGS_CONFIG_PATH_PATTERN;
import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN;
import static com.icthh.xm.gate.config.Constants.AUTH_RESPONSE_FIELD_IDP_TOKEN;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static com.icthh.xm.gate.security.oauth2.IdpAuthenticationSuccessHandler.*;
import static com.icthh.xm.gate.security.oauth2.IdpTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@ExtendWith(MockitoExtension.class)
public class IdpAuthenticationSuccessHandlerUnitTest {

    private static final String TENANT_REPLACE_PATTERN = "{tenant}";
    @Mock
    private RestTemplate restTemplate;

    private final JHipsterProperties jhipsterProperties = new JHipsterProperties();
    private final TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();
    private final IdpClientRepository clientRegistrationRepository = new IdpClientRepository(tenantContextHolder);
    private final IdpConfigRepository idpConfigRepository = new IdpConfigRepository(clientRegistrationRepository);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private IdpAuthenticationSuccessHandler idpAuthenticationSuccessHandler;

    @BeforeEach
    void setUp() {
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
            anyString(),
            eq(HttpMethod.POST),
            any(),
            (ParameterizedTypeReference<Map<String, Object>>) any()))
            .thenReturn(buildResponce(true));

        this.idpAuthenticationSuccessHandler =
            new IdpAuthenticationSuccessHandler(objectMapper, restTemplate, tenantContextHolder, idpConfigRepository, jhipsterProperties);

        MockHttpServletResponse response = new MockHttpServletResponse();
        idpAuthenticationSuccessHandler.onAuthenticationSuccess(null, response, buildAuthentication("email"));

        //Capture and validate args
        ArgumentCaptor<String> accessTokenUriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> uaaTokenRequestCaptor = ArgumentCaptor.forClass(Object.class);

        verify(restTemplate, times(1)).exchange(
            accessTokenUriCaptor.capture(),
            eq(HttpMethod.POST),
            (HttpEntity<?>) uaaTokenRequestCaptor.capture(),
            (ParameterizedTypeReference<Map<String, Object>>) any()
        );

        HttpEntity<MultiValueMap<String, String>> uaaTokenRequestCaptorValues = (HttpEntity<MultiValueMap<String, String>>) uaaTokenRequestCaptor.getValue();

        assertEquals("http://uaa.com", accessTokenUriCaptor.getValue());
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

    private ResponseEntity<Map<String, Object>> buildResponce(boolean buildValidResponse) {
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

    private IdpPublicConfig registerPublicConfigs(String clientKeyPrefix,
                                                  String tenantKey,
                                                  int clientsAmount,
                                                  boolean buildValidConfig,
                                                  boolean onInit,
                                                  boolean isStateful) throws JsonProcessingException {
        String publicSettingsConfigPath = IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN.replace(TENANT_REPLACE_PATTERN, tenantKey);

        IdpPublicConfig idpPublicConfig = buildPublicConfig(clientKeyPrefix, clientsAmount, "client-id", buildValidConfig);
        if (isStateful) {
            idpPublicConfig.getConfig().getIdpAccessTokenInclusion().setStateful(true);
        }
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);

        if (onInit) {
            idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);
        } else {
            idpConfigRepository.onRefresh(publicSettingsConfigPath, publicConfigAsString);
        }

        return idpPublicConfig;
    }

    private IdpPrivateConfig registerPrivateConfigs(String clientKeyPrefix,
                                                    String tenantKey,
                                                    int clientsAmount,
                                                    boolean buildValidConfig, boolean onInit) throws JsonProcessingException {
        String privateSettingsConfigPath = IDP_PRIVATE_SETTINGS_CONFIG_PATH_PATTERN.replace(TENANT_REPLACE_PATTERN, tenantKey);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(clientKeyPrefix, clientsAmount, buildValidConfig);
        String privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);

        if (onInit) {
            idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);
        } else {
            idpConfigRepository.onRefresh(privateSettingsConfigPath, privateConfigAsString);
        }
        return idpPrivateConfig;
    }

    private void validateRegistration(String tenantKey, String clientKeyPrefix, int clientsAmount, IdpPublicConfig idpPublicConfig, IdpPrivateConfig idpPrivateConfig) {
        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);

        for (int i = 0; i < clientsAmount; i++) {
            String registrationId = clientKeyPrefix + i;
            ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);

            assertNotNull(clientRegistration);
            validateClientRegistration(registrationId, idpPublicConfig, idpPrivateConfig, clientRegistration);
            validateInMemoryIdpConfigs(tenantKey, registrationId);
        }

        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    private void validateClientRegistration(String registrationId,
                                            IdpPublicConfig idpPublicConfig,
                                            IdpPrivateConfig idpPrivateConfig,
                                            ClientRegistration registration) {
        IdpPublicClientConfig idpPublicClientConfig = idpPublicConfig.getConfig()
            .getClients()
            .stream()
            .filter(config -> registrationId.equals(config.getKey())).findAny()
            .orElseThrow();

        IdpPrivateClientConfig idpPrivateClientConfig = idpPrivateConfig.getConfig()
            .getClients()
            .stream()
            .filter(config -> registrationId.equals(config.getKey())).findAny()
            .orElseThrow();

        assertEquals(registrationId, registration.getRegistrationId());
        assertEquals(idpPublicClientConfig.getClientId(), registration.getClientId());
        assertEquals(ClientAuthenticationMethod.BASIC, registration.getClientAuthenticationMethod());
        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, registration.getAuthorizationGrantType());
        assertEquals(idpPublicClientConfig.getRedirectUri(), registration.getRedirectUri());
        assertEquals(idpPrivateClientConfig.getScope(), registration.getScopes());
        assertEquals(registrationId, registration.getClientName());
        assertEquals(idpPrivateClientConfig.getClientSecret(), registration.getClientSecret());
    }

    private void validateInMemoryIdpConfigs(String tenantKey, String clientRegistrationId) {
        IdpConfigContainer idpConfigContainer = idpConfigRepository.getIdpClientConfigs()
            .getOrDefault(tenantKey, Collections.emptyMap())
            .get(clientRegistrationId);

        assertNotNull(idpConfigContainer);
        assertNotNull(idpConfigContainer.getIdpPublicClientConfig());
        assertNotNull(idpConfigContainer.getIdpPrivateClientConfig());
    }

}
