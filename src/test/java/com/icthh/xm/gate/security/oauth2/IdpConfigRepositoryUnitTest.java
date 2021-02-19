package com.icthh.xm.gate.security.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.idp.IdpPrivateConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.gate.domain.idp.IdpConfigContainer;
import com.icthh.xm.commons.domain.idp.IdpPublicConfig;
import com.icthh.xm.commons.domain.idp.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import com.icthh.xm.commons.domain.idp.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.commons.domain.idp.IdpPublicConfig.IdpConfigContainer.Features;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class IdpConfigRepositoryUnitTest {

    private final TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();

    private final IdpClientHolder clientRegistrationRepository = new IdpClientHolder(tenantContextHolder);

    private final IdpConfigRepository idpConfigRepository = new IdpConfigRepository(clientRegistrationRepository);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void test_shouldNotRegisterAnyTenantClient() throws JsonProcessingException {
        String publicSettingsConfigPath = "/config/tenants/tenant1/webapp/public/idp-config-public.yml";

        String tenantKey = "tenant1";
        String registrationId = "Auth0_";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId, 1);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        assertNull(clientRegistrationRepository.findByRegistrationId(registrationId));
    }

    @Test
    public void test_shouldSuccessfullyRegisterExactOneTenantClient() throws JsonProcessingException {
        // FIXME: please correct file names according to latest renames in all tests.
        String publicSettingsConfigPath = "/config/tenants/tenant1/webapp/public/idp-config-public.yml";
        String privateSettingsConfigPath = "/config/tenants/tenant1/idp-config-private.yml";

        String tenantKey = "tenant1";
        String registrationId = "Auth0_";
        // FIXME: next 7 lines are repeated almost in any test. I think it worth to extract them to method and use it.
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId, 1);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(registrationId, 1);
        String privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        validateRegistration(tenantKey, "Auth0_0", idpPublicConfig, idpPrivateConfig);
    }

    @Test
    public void test_shouldSuccessfullyRegisterOnlyOneTenantClient() throws JsonProcessingException {
        String publicSettingsConfigPath = "/config/tenants/tenant1/webapp/public/idp-config-public.yml";
        String privateSettingsConfigPath = "/config/tenants/tenant1/idp-config-private.yml";

        String tenantKey = "tenant1";
        String registrationId = "Auth0_";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId, 1);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(registrationId, 1);
        String privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        validateRegistration(tenantKey, "Auth0_0", idpPublicConfig, idpPrivateConfig);

        registrationId = "Auth0_1";
        idpPrivateConfig = buildPrivateConfig(registrationId, 1);
        privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        assertNull(clientRegistrationRepository.findByRegistrationId("Auth0_1"));
    }

    @Test
    public void test_shouldSuccessfullyRegisterTwoClientsForTenant() throws JsonProcessingException {
        String publicSettingsConfigPath = "/config/tenants/tenant1/webapp/public/idp-config-public.yml";
        String privateSettingsConfigPath = "/config/tenants/tenant1/idp-config-private.yml";

        String tenantKey = "tenant1";
        String registrationId = "Auth0_";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId, 2);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(registrationId, 2);
        String privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        validateRegistration(tenantKey, "Auth0_0", idpPublicConfig, idpPrivateConfig);
        validateRegistration(tenantKey, "Auth0_1", idpPublicConfig, idpPrivateConfig);

    }

    @Test
    public void test_shouldRemoveOneRegisteredClientForTenant() throws JsonProcessingException {
        String publicSettingsConfigPath = "/config/tenants/tenant1/webapp/public/idp-config-public.yml";
        String privateSettingsConfigPath = "/config/tenants/tenant1/idp-config-private.yml";

        String tenantKey = "tenant1";

        //register two tenant clients
        String registrationId = "Auth0_";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId, 2);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(registrationId, 2);
        String privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        validateRegistration(tenantKey, "Auth0_0", idpPublicConfig, idpPrivateConfig);
        validateRegistration(tenantKey, "Auth0_1", idpPublicConfig, idpPrivateConfig);

        //register one tenant client instead of two
        idpPublicConfig = buildPublicConfig(registrationId, 1);
        publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onRefresh(publicSettingsConfigPath, publicConfigAsString);

        idpPrivateConfig = buildPrivateConfig(registrationId, 1);
        privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onRefresh(privateSettingsConfigPath, privateConfigAsString);

        validateRegistration(tenantKey, "Auth0_0", idpPublicConfig, idpPrivateConfig);

        assertEquals(1, clientRegistrationRepository.findByTenantKey(tenantKey).size());

    }

    @Test
    public void test_shouldFixInMemoryClientConfigurationAndRegisterClientForTenant() throws JsonProcessingException {
        String publicSettingsConfigPath = "/config/tenants/tenant1/webapp/public/idp-config-public.yml";
        String privateSettingsConfigPath = "/config/tenants/tenant1/idp-config-private.yml";

        String tenantKey = "tenant1";

        //unsuccessful registration for tenant client
        String registrationId = "Auth0_";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId, 1);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        assertNull(clientRegistrationRepository.findByTenantKey(tenantKey));

        //register one tenant client with full configuration
        idpPublicConfig = buildPublicConfig(registrationId, 1);
        publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onRefresh(publicSettingsConfigPath, publicConfigAsString);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(registrationId, 1);
        String privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);

        idpConfigRepository.onRefresh(privateSettingsConfigPath, privateConfigAsString);

        validateRegistration(tenantKey, "Auth0_0", idpPublicConfig, idpPrivateConfig);

        assertEquals(1, clientRegistrationRepository.findByTenantKey(tenantKey).size());

    }

    @Test
    public void test_shouldRemoveAllRegisteredClientForTenant() throws JsonProcessingException {
        String publicSettingsConfigPath = "/config/tenants/tenant1/webapp/public/idp-config-public.yml";
        String privateSettingsConfigPath = "/config/tenants/tenant1/idp-config-private.yml";

        String tenantKey = "tenant1";

        //register two clients for tenant
        String registrationId = "Auth0_";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId, 2);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(registrationId, 2);
        String privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        validateRegistration(tenantKey, "Auth0_0", idpPublicConfig, idpPrivateConfig);
        validateRegistration(tenantKey, "Auth0_1", idpPublicConfig, idpPrivateConfig);

        //remove all clients registration for tenant
        idpConfigRepository.onRefresh(publicSettingsConfigPath, "idp:");

        idpConfigRepository.onRefresh(privateSettingsConfigPath, "idp:");

        assertNull(clientRegistrationRepository.findByTenantKey(tenantKey));

    }

    @Test
    public void test_shouldSuccessfullyRegisterOneClientPerTenant() throws JsonProcessingException {
        String publicSettingsConfigPath = "/config/tenants/tenant1/webapp/public/idp-config-public.yml";
        String privateSettingsConfigPath = "/config/tenants/tenant1/idp-config-private.yml";

        String tenantKey = "tenant1";
        String registrationId = "Auth0_";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId, 1);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(registrationId, 1);
        String privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        validateRegistration(tenantKey, "Auth0_0", idpPublicConfig, idpPrivateConfig);

        publicSettingsConfigPath = "/config/tenants/tenant2/webapp/public/idp-config-public.yml";
        privateSettingsConfigPath = "/config/tenants/tenant2/idp-config-private.yml";

        tenantKey = "tenant2";
        registrationId = "Auth0_";

        idpPublicConfig = buildPublicConfig(registrationId, 1);
        publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        idpPrivateConfig = buildPrivateConfig(registrationId, 1);
        privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        validateRegistration(tenantKey, "Auth0_0", idpPublicConfig, idpPrivateConfig);

    }

    private void validateRegistration(String tenantKey, String registrationId, IdpPublicConfig idpPublicConfig, IdpPrivateConfig idpPrivateConfig) {
        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);

        assertNotNull(clientRegistration);
        validateClientRegistration(registrationId, idpPublicConfig, idpPrivateConfig, clientRegistration);
        validateInMemoryIdpConfigs(tenantKey, registrationId);
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

    private IdpPublicConfig buildPublicConfig(String key, int clientsAmount) {
        IdpPublicConfig idpPublicConfig = new IdpPublicConfig();
        IdpPublicConfig.IdpConfigContainer config = new IdpPublicConfig.IdpConfigContainer();

        config.setDirectLogin(true);
        List<IdpPublicClientConfig> idpPublicClientConfigs = new ArrayList<>();
        for (int i = 0; i < clientsAmount; i++) {
            idpPublicClientConfigs.add(buildIdpPublicClientConfig(key + i));
        }
        config.setClients(idpPublicClientConfigs);
        config.setFeatures(buildFeatures());

        idpPublicConfig.setConfig(config);

        return idpPublicConfig;
    }

    private IdpPublicClientConfig buildIdpPublicClientConfig(String key) {
        IdpPublicClientConfig idpPublicClientConfig = new IdpPublicClientConfig();
        IdpPublicClientConfig.OpenIdConfig openIdConfig = new IdpPublicClientConfig.OpenIdConfig();

        idpPublicClientConfig.setKey(key);
        idpPublicClientConfig.setClientId("client-id");
        idpPublicClientConfig.setName(key);
        idpPublicClientConfig.setRedirectUri("http://localhost:4200");

        openIdConfig.setAuthorizationEndpoint(buildAuthorizationEndpoint());
        openIdConfig.setTokenEndpoint(buildTokenEndpoint());
        openIdConfig.setUserinfoEndpoint(buildUserInfoEndpoint());
        openIdConfig.setEndSessionEndpoint(buildEndSessionEndpoint());
        openIdConfig.setJwksEndpoint(buildJwksEndpoint());

        idpPublicClientConfig.setOpenIdConfig(openIdConfig);

        return idpPublicClientConfig;
    }

    private IdpPublicClientConfig.OpenIdConfig.UserInfoEndpoint buildUserInfoEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.UserInfoEndpoint userinfoEndpoint = new IdpPublicClientConfig.OpenIdConfig.UserInfoEndpoint();
        userinfoEndpoint.setUri("https://idp1.com/userinfo");
        userinfoEndpoint.setUserNameAttributeName("email");
        return userinfoEndpoint;
    }

    private IdpPublicClientConfig.OpenIdConfig.BaseEndpoint buildEndSessionEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.BaseEndpoint userinfoEndpoint = new IdpPublicClientConfig.OpenIdConfig.BaseEndpoint();
        userinfoEndpoint.setUri("https://idp1.com/logout");
        return userinfoEndpoint;
    }

    private IdpPublicClientConfig.OpenIdConfig.BaseEndpoint buildJwksEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.BaseEndpoint userinfoEndpoint = new IdpPublicClientConfig.OpenIdConfig.BaseEndpoint();
        userinfoEndpoint.setUri("https://idp1.com/.well-known/jwks.json");
        return userinfoEndpoint;
    }

    private IdpPublicClientConfig.OpenIdConfig.TokenEndpoint buildTokenEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.TokenEndpoint tokenEndpoint = new IdpPublicClientConfig.OpenIdConfig.TokenEndpoint();
        tokenEndpoint.setUri("https://idp1.com/oauth/token");
        tokenEndpoint.setGrantType("authorization_code");
        return tokenEndpoint;
    }

    private IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint buildAuthorizationEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint authorizationEndpoint = new IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint();

        authorizationEndpoint.setUri("https://idp1.com/authorize");
        authorizationEndpoint.setResponseType("code");
        authorizationEndpoint.setAdditionalParams(Map.of("connection", "google-oauth2"));

        IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint.Features features = new IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint.Features();
        features.setState(true);
        authorizationEndpoint.setFeatures(features);

        return authorizationEndpoint;
    }

    private Features buildFeatures() {
        Features features = new Features();

        features.setPkce(false);
        features.setStateful(false);

        Features.Bearirng bearirng = new Features.Bearirng();
        bearirng.setEnabled(true);
        bearirng.setIdpTokenHeader("Authorization");
        bearirng.setXmTokenHeader("X-Authorization");
        features.setBearirng(bearirng);

        return features;
    }


    private IdpPrivateConfig buildPrivateConfig(String key, int clientsAmount) {
        IdpPrivateConfig idpPrivateConfig = new IdpPrivateConfig();
        IdpPrivateConfig.IdpConfigContainer config = new IdpPrivateConfig.IdpConfigContainer();

        List<IdpPrivateClientConfig> idpPrivateClientConfigs = new ArrayList<>();
        for (int i = 0; i < clientsAmount; i++) {
            idpPrivateClientConfigs.add(buildIdpPrivateClientConfigs(key + i));
        }

        config.setClients(idpPrivateClientConfigs);

        idpPrivateConfig.setConfig(config);

        return idpPrivateConfig;
    }

    private IdpPrivateClientConfig buildIdpPrivateClientConfigs(String key) {
        IdpPrivateClientConfig idpPrivateClientConfig = new IdpPrivateClientConfig();

        idpPrivateClientConfig.setKey(key);
        idpPrivateClientConfig.setClientSecret("client-secret");
        idpPrivateClientConfig.setScope(Set.of("openid", "profile", "email"));

        return idpPrivateClientConfig;
    }
}
