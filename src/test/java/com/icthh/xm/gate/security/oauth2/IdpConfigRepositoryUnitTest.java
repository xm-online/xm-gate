package com.icthh.xm.gate.security.oauth2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.gate.domain.idp.IdpConfigContainer;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN;
import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_PRIVATE_SETTINGS_CONFIG_PATH_PATTERN;
import static com.icthh.xm.gate.security.oauth2.IdpTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class IdpConfigRepositoryUnitTest {

    private static final String TENANT_KEY_REPLACE_PATTERN = "{tenant}";
    private final TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();
    private final IdpClientRepository clientRegistrationRepository = new IdpClientRepository(tenantContextHolder);
    private final IdpConfigRepository idpConfigRepository = new IdpConfigRepository(clientRegistrationRepository);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void test_shouldNotRegisterAnyTenantClient() throws JsonProcessingException {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";

        registerPublicConfigs(clientKeyPrefix, tenantKey, 1, false, true);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        assertNull(clientRegistrationRepository.findByRegistrationId(clientKeyPrefix));
    }

    @Test
    public void test_shouldSuccessfullyRegisterExactOneTenantClient() throws JsonProcessingException {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";

        int clientsAmount = 1;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);
    }

    @Test
    public void test_shouldSuccessfullyRegisterOnlyOneTenantClient() throws JsonProcessingException {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 1;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        clientKeyPrefix = "Auth0_1";
        registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        assertNull(clientRegistrationRepository.findByRegistrationId(clientKeyPrefix));
    }

    @Test
    public void test_shouldSuccessfullyRegisterTwoClientsForTenant() throws JsonProcessingException {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 2;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);
    }

    @Test
    public void test_shouldRemoveOneRegisteredClientForTenant() throws JsonProcessingException {
        String tenantKey = "tenant1";

        //register two tenant clients
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 2;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        //register one tenant client instead of two
        clientsAmount = 1;
        idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false);
        idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        assertEquals(1, clientRegistrationRepository.findByTenantKey(tenantKey).size());
    }

    @Test
    public void test_shouldFixInMemoryClientConfigurationAndRegisterClientForTenant() throws JsonProcessingException {
        String tenantKey = "tenant1";

        //unsuccessful registration for tenant client
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 1;

        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, false, true);

        assertNull(clientRegistrationRepository.findByTenantKey(tenantKey));

        //register one tenant client with full configuration
        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        assertEquals(1, clientRegistrationRepository.findByTenantKey(tenantKey).size());
    }

    @Test
    public void test_shouldRemoveAllRegisteredClientForTenant() throws JsonProcessingException {
        String tenantKey = "tenant1";

        //register two clients for tenant
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 2;
        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        //remove all clients registration for tenant

        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, false, false);
        registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, false, false);

        assertNull(clientRegistrationRepository.findByTenantKey(tenantKey));
    }

    @Test
    public void test_shouldSuccessfullyRegisterOneClientPerTenant() throws JsonProcessingException {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 1;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        tenantKey = "tenant2";
        clientKeyPrefix = "Auth0_";

        idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);
        idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);
    }

    private IdpPublicConfig registerPublicConfigs(String clientKeyPrefix,
                                                  String tenantKey,
                                                  int clientsAmount,
                                                  boolean buildValidConfig, boolean onInit) throws JsonProcessingException {
        String publicSettingsConfigPath = IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN.replace(TENANT_KEY_REPLACE_PATTERN, tenantKey);

        IdpPublicConfig idpPublicConfig = buildPublicConfig(clientKeyPrefix, clientsAmount, "client-id", buildValidConfig);
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
        String privateSettingsConfigPath = IDP_PRIVATE_SETTINGS_CONFIG_PATH_PATTERN.replace(TENANT_KEY_REPLACE_PATTERN, tenantKey);

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
