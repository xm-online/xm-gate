package com.icthh.xm.gate.security.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.gate.domain.idp.IdpPrivateConfig;
import com.icthh.xm.gate.domain.idp.IdpPublicConfig;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.icthh.xm.gate.domain.idp.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.gate.domain.idp.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
public class IdpConfigRepositoryUnitTest {

    private final IdpClientHolder clientRegistrationRepository = mock(IdpClientHolder.class);

    private final IdpConfigRepository idpConfigRepository = new IdpConfigRepository(clientRegistrationRepository);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void test_shouldNotRegisterAnyTenantClient() throws JsonProcessingException {
        String publicSettingsConfigPath = "/config/tenants/XM/webapp/settings-public.yml";

        String registrationId = "Auth0_1";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        verify(clientRegistrationRepository, times(0)).setRegistrations(any());
    }

    @Test
    public void test_shouldSuccessfullyRegisterExactOneTenantClient() throws JsonProcessingException {
        String publicSettingsConfigPath = "/config/tenants/XM/webapp/settings-public.yml";
        String privateSettingsConfigPath = "/config/tenants/XM/idp-config.yml";

        String tenantKey = "XM";
        String registrationId = "Auth0_1";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(registrationId);
        String privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClientRegistration>> clientRegistrations = ArgumentCaptor.forClass(List.class);

        captureAndValidateClient(tenantKey, registrationId, idpPublicConfig, idpPrivateConfig, clientRegistrations, 1);
    }

    @Test
    public void test_shouldSuccessfullyRegisterOnlyOneTenantClient() throws JsonProcessingException {
        String publicSettingsConfigPath = "/config/tenants/XM/webapp/settings-public.yml";
        String privateSettingsConfigPath = "/config/tenants/XM/idp-config.yml";

        String tenantKey = "XM";
        String registrationId_1 = "Auth0_1";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId_1);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(registrationId_1);
        String privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        String registrationId_2 = "Auth0_2";
        idpPrivateConfig = buildPrivateConfig(registrationId_2);
        privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClientRegistration>> clientRegistrations = ArgumentCaptor.forClass(List.class);

        captureAndValidateClient(tenantKey, registrationId_1, idpPublicConfig, idpPrivateConfig, clientRegistrations, 1);
    }

    @Test
    public void test_shouldSuccessfullyRegisterTwoClientsForTenant() throws JsonProcessingException {
        String publicSettingsConfigPath = "/config/tenants/XM/webapp/settings-public.yml";
        String privateSettingsConfigPath = "/config/tenants/XM/idp-config.yml";

        String tenantKey = "XM";
        String registrationId = "Auth0_1";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(registrationId);
        String privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClientRegistration>> clientRegistrationListCaptor = ArgumentCaptor.forClass(List.class);
        captureAndValidateClient(tenantKey, registrationId, idpPublicConfig, idpPrivateConfig, clientRegistrationListCaptor, 1);

        String registrationId2 = "Auth0_2";

        idpPublicConfig = buildPublicConfig(registrationId2);
        publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        idpPrivateConfig = buildPrivateConfig(registrationId2);
        privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);


        captureAndValidateClient(tenantKey, registrationId2, idpPublicConfig, idpPrivateConfig,
            clientRegistrationListCaptor, 2);

    }

    @Test
    public void test_shouldSuccessfullyRegisterOneClientPerTenant() throws JsonProcessingException {
        String publicSettingsConfigPath = "/config/tenants/XM/webapp/settings-public.yml";
        String privateSettingsConfigPath = "/config/tenants/XM/idp-config.yml";

        String tenantKey = "XM";
        String registrationId = "Auth0_1";
        IdpPublicConfig idpPublicConfig = buildPublicConfig(registrationId);
        String publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(registrationId);
        String privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClientRegistration>> clientRegistrationListCaptor = ArgumentCaptor.forClass(List.class);
        captureAndValidateClient(tenantKey, registrationId, idpPublicConfig, idpPrivateConfig, clientRegistrationListCaptor, 1);

        publicSettingsConfigPath = "/config/tenants/WH/webapp/settings-public.yml";
        privateSettingsConfigPath = "/config/tenants/WH/idp-config.yml";

        tenantKey = "WH";
        String registrationId2 = "Auth0_1";

        idpPublicConfig = buildPublicConfig(registrationId2);
        publicConfigAsString = objectMapper.writeValueAsString(idpPublicConfig);
        idpConfigRepository.onInit(publicSettingsConfigPath, publicConfigAsString);

        idpPrivateConfig = buildPrivateConfig(registrationId2);
        privateConfigAsString = objectMapper.writeValueAsString(idpPrivateConfig);
        idpConfigRepository.onInit(privateSettingsConfigPath, privateConfigAsString);


        captureAndValidateClient(tenantKey, registrationId2, idpPublicConfig, idpPrivateConfig,
            clientRegistrationListCaptor, 2);

    }

    private void captureAndValidateClient(String tenantKey, String registrationId,
                                          IdpPublicConfig idpPublicConfig,
                                          IdpPrivateConfig idpPrivateConfig,
                                          ArgumentCaptor<List<ClientRegistration>> clientRegistrationListCaptor, int times) {
        verify(clientRegistrationRepository, times(times)).setRegistrations(clientRegistrationListCaptor.capture());
        List<ClientRegistration> clientRegistrations = clientRegistrationListCaptor.getValue();

        assertEquals(1, clientRegistrations.size());
        ClientRegistration registration = clientRegistrations.get(0);

        validateClientRegistration(tenantKey, registrationId, idpPublicConfig, idpPrivateConfig, registration);
    }

    private void validateClientRegistration(String tenantKey, String registrationId, IdpPublicConfig idpPublicConfig, IdpPrivateConfig idpPrivateConfig, ClientRegistration registration) {
        IdpPublicClientConfig idpPublicClientConfig = idpPublicConfig.getConfig().getClients().get(0);
        IdpPrivateClientConfig idpPrivateClientConfig = idpPrivateConfig.getConfig().getClients().get(0);

        assertEquals(IdpUtils.buildCompositeIdpKey(tenantKey, registrationId), registration.getRegistrationId());
        assertEquals(idpPublicClientConfig.getClientId(), registration.getClientId());
        assertEquals(ClientAuthenticationMethod.BASIC, registration.getClientAuthenticationMethod());
        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, registration.getAuthorizationGrantType());
        assertEquals(idpPublicClientConfig.getRedirectUri(), registration.getRedirectUriTemplate());
        assertEquals(idpPrivateClientConfig.getScope(), registration.getScopes());
        assertEquals(IdpUtils.buildCompositeIdpKey(tenantKey, registrationId), registration.getClientName());
        assertEquals(idpPrivateClientConfig.getClientSecret(), registration.getClientSecret());
    }

    private IdpPublicConfig buildPublicConfig(String key) {
        IdpPublicConfig idpPublicConfig = new IdpPublicConfig();
        IdpPublicConfig.IdpConfigContainer config = new IdpPublicConfig.IdpConfigContainer();

        config.setDirectLogin(true);

        config.setClients(List.of(buildIdpPublicClientConfig(key)));

        idpPublicConfig.setConfig(config);

        return idpPublicConfig;
    }

    private IdpPublicClientConfig buildIdpPublicClientConfig(String key) {
        IdpPublicClientConfig idpPublicClientConfig = new IdpPublicClientConfig();

        idpPublicClientConfig.setKey(key);
        idpPublicClientConfig.setClientId("VtRxEs7qN4oSte7Jap7gXn83mfK1Ww20");
        idpPublicClientConfig.setRedirectUri("http://localhost:4200");

        idpPublicClientConfig.setFeatures(buildFeatures());

        idpPublicClientConfig.setAuthorizationEndpoint(buildAuthorizationEndpoint());
        idpPublicClientConfig.setTokenEndpoint(buildTokenEndpoint());
        idpPublicClientConfig.setUserinfoEndpoint(buildUserInfoEndpoint());
        idpPublicClientConfig.setEndSessionEndpoint(buildEndSessionEndpoint());
        idpPublicClientConfig.setJwksEndpoint(buildJwksEndpoint());

        return idpPublicClientConfig;
    }

    private IdpPublicClientConfig.BaseEndpoint buildUserInfoEndpoint() {
        IdpPublicClientConfig.BaseEndpoint userinfoEndpoint = new IdpPublicClientConfig.BaseEndpoint();
        userinfoEndpoint.setUri("https://idp1.com/userinfo");
        return userinfoEndpoint;
    }

    private IdpPublicClientConfig.BaseEndpoint buildEndSessionEndpoint() {
        IdpPublicClientConfig.BaseEndpoint userinfoEndpoint = new IdpPublicClientConfig.BaseEndpoint();
        userinfoEndpoint.setUri("https://idp1.com/logout");
        return userinfoEndpoint;
    }

    private IdpPublicClientConfig.BaseEndpoint buildJwksEndpoint() {
        IdpPublicClientConfig.BaseEndpoint userinfoEndpoint = new IdpPublicClientConfig.BaseEndpoint();
        userinfoEndpoint.setUri("https://idp1.com/.well-known/jwks.json");
        return userinfoEndpoint;
    }

    private IdpPublicClientConfig.TokenEndpoint buildTokenEndpoint() {
        IdpPublicClientConfig.TokenEndpoint tokenEndpoint = new IdpPublicClientConfig.TokenEndpoint();
        tokenEndpoint.setUri("https://idp1.com/oauth/token");
        tokenEndpoint.setGrantType("authorization_code");
        return tokenEndpoint;
    }

    private IdpPublicClientConfig.AuthorizationEndpoint buildAuthorizationEndpoint() {
        IdpPublicClientConfig.AuthorizationEndpoint authorizationEndpoint = new IdpPublicClientConfig.AuthorizationEndpoint();

        authorizationEndpoint.setUri("https://idp1.com/authorize");
        authorizationEndpoint.setResponseType("code");
        authorizationEndpoint.setAdditionalParams(Map.of("connection", "google-oauth2"));

        IdpPublicClientConfig.AuthorizationEndpoint.Features features = new IdpPublicClientConfig.AuthorizationEndpoint.Features();
        features.setState(true);
        authorizationEndpoint.setFeatures(features);

        return authorizationEndpoint;
    }

    private IdpPublicClientConfig.Features buildFeatures() {
        IdpPublicClientConfig.Features features = new IdpPublicClientConfig.Features();

        features.setPkce(false);
        features.setStateful(false);

        IdpPublicClientConfig.Features.Bearirng bearirng = new IdpPublicClientConfig.Features.Bearirng();
        bearirng.setEnabled(true);
        bearirng.setIdpTokenHeader("Authorization");
        bearirng.setXmTokenHeader("X-Authorization");
        features.setBearirng(bearirng);

        return features;
    }


    private IdpPrivateConfig buildPrivateConfig(String key) {
        IdpPrivateConfig idpPrivateConfig = new IdpPrivateConfig();
        IdpPrivateConfig.IdpConfigContainer config = new IdpPrivateConfig.IdpConfigContainer();

        config.setClients(List.of(buildIdpPrivateClientConfigs(key)));

        idpPrivateConfig.setConfig(config);

        return idpPrivateConfig;
    }

    private IdpPrivateClientConfig buildIdpPrivateClientConfigs(String key) {
        IdpPrivateClientConfig idpPrivateClientConfig = new IdpPrivateClientConfig();

        idpPrivateClientConfig.setKey(key);
        idpPrivateClientConfig.setClientSecret("NHOsxzwEBgflBHuGF-mF9NkF8HI5kotVkBJYrpTPsZf0s9Js5klBrJ5bdROjMHLZ");
        idpPrivateClientConfig.setScope(Set.of("openid", "profile", "email"));
        idpPrivateClientConfig.setAdditionalParams(Map.of("audience", "https://idp1.com/api/v2/"));

        return idpPrivateClientConfig;
    }
}
