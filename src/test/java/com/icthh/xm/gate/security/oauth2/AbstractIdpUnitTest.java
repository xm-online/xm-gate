package com.icthh.xm.gate.security.oauth2;

import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_PRIVATE_SETTINGS_CONFIG_PATH_PATTERN;
import static com.icthh.xm.commons.domain.idp.IdpConstants.IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN;
import static com.icthh.xm.gate.security.oauth2.IdpTestUtils.buildPrivateConfig;
import static com.icthh.xm.gate.security.oauth2.IdpTestUtils.buildPublicConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import com.icthh.xm.gate.domain.idp.IdpConfigContainer;
import lombok.SneakyThrows;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.Collections;

public abstract class AbstractIdpUnitTest {

    static final String TENANT_KEY_REPLACE_PATTERN = "{tenant}";

    protected final TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();
    protected final IdpClientRepository clientRegistrationRepository = new IdpClientRepository(tenantContextHolder);
    protected final IdpConfigRepository idpConfigRepository = new IdpConfigRepository(clientRegistrationRepository);
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected IdpPublicConfig registerPublicConfigs(String clientKeyPrefix,
                                                    String tenantKey,
                                                    int clientsAmount,
                                                    boolean buildValidConfig,
                                                    boolean onInit,
                                                    boolean isStateful) {
        String publicSettingsConfigPath = IDP_PUBLIC_SETTINGS_CONFIG_PATH_PATTERN
            .replace(TENANT_KEY_REPLACE_PATTERN, tenantKey);

        IdpPublicConfig idpPublicConfig = buildPublicConfig(clientKeyPrefix,
                                                            clientsAmount,
                                                            "client-id",
                                                            buildValidConfig);
        idpPublicConfig.getConfig().getFeatures().setStateful(isStateful);
        refreshConfig(onInit, publicSettingsConfigPath, idpPublicConfig);

        return idpPublicConfig;
    }

    protected IdpPrivateConfig registerPrivateConfigs(String clientKeyPrefix,
                                                      String tenantKey,
                                                      int clientsAmount,
                                                      boolean buildValidConfig,
                                                      boolean onInit) {
        String privateSettingsConfigPath = IDP_PRIVATE_SETTINGS_CONFIG_PATH_PATTERN
            .replace(TENANT_KEY_REPLACE_PATTERN, tenantKey);

        IdpPrivateConfig idpPrivateConfig = buildPrivateConfig(clientKeyPrefix, clientsAmount, buildValidConfig);

        refreshConfig(onInit, privateSettingsConfigPath, idpPrivateConfig);

        return idpPrivateConfig;
    }

    protected void validateRegistration(String tenantKey, String clientKeyPrefix, int clientsAmount,
                                        IdpPublicConfig idpPublicConfig, IdpPrivateConfig idpPrivateConfig) {
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

    private void validateInMemoryIdpConfigs(String tenantKey,
                                            String clientRegistrationId) {
        IdpConfigContainer idpConfigContainer = idpConfigRepository.getIdpClientConfigs()
                                                                   .getOrDefault(tenantKey, Collections.emptyMap())
                                                                   .get(clientRegistrationId);

        assertNotNull(idpConfigContainer);
        assertNotNull(idpConfigContainer.getIdpPublicClientConfig());
        assertNotNull(idpConfigContainer.getIdpPrivateClientConfig());
    }

    private void validateClientRegistration(String registrationId,
                                            IdpPublicConfig idpPublicConfig,
                                            IdpPrivateConfig idpPrivateConfig,
                                            ClientRegistration registration) {
        IdpPublicClientConfig idpPublicClientConfig = idpPublicConfig.getConfig()
                                                                     .getClients()
                                                                     .stream()
                                                                     .filter(config -> registrationId.equals(config.getKey()))
                                                                     .findAny()
                                                                     .orElseThrow();

        IdpPrivateClientConfig idpPrivateClientConfig = idpPrivateConfig.getConfig()
                                                                        .getClients()
                                                                        .stream()
                                                                        .filter(config -> registrationId.equals(config.getKey()))
                                                                        .findAny()
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

    @SneakyThrows
    private void refreshConfig(final boolean onInit, final String configPath, final Object content) {
        String contentString = objectMapper.writeValueAsString(content);

        if (onInit) {
            idpConfigRepository.onInit(configPath, contentString);
        } else {
            idpConfigRepository.onRefresh(configPath, contentString);
        }
    }
}
