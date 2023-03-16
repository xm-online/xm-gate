package com.icthh.xm.gate.security.oauth2;

import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class IdpConfigRepositoryUnitTest extends AbstractIdpUnitTest {

    @Test
    public void test_shouldNotRegisterAnyTenantClient() {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";

        registerPublicConfigs(clientKeyPrefix, tenantKey, 1, false, true, false);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        assertNull(clientRegistrationRepository.findByRegistrationId(clientKeyPrefix));
    }

    @Test
    public void test_shouldSuccessfullyRegisterExactOneTenantClient() {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";

        int clientsAmount = 1;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true, false);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);
    }

    @Test
    public void test_shouldSuccessfullyRegisterOnlyOneTenantClient() {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 1;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true, false);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        clientKeyPrefix = "Auth0_1";
        registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        assertNull(clientRegistrationRepository.findByRegistrationId(clientKeyPrefix));
    }

    @Test
    public void test_shouldSuccessfullyRegisterTwoClientsForTenant() {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 2;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true, false);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);
    }

    @Test
    public void test_shouldRemoveOneRegisteredClientForTenant() {
        String tenantKey = "tenant1";

        //register two tenant clients
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 2;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true, false);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        //register one tenant client instead of two
        clientsAmount = 1;
        idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false, false);
        idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        assertEquals(1, clientRegistrationRepository.findByTenantKey(tenantKey).size());
    }

    @Test
    public void test_shouldFixInMemoryClientConfigurationAndRegisterClientForTenant() {
        String tenantKey = "tenant1";

        //unsuccessful registration for tenant client
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 1;

        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, false, true, false);

        assertNull(clientRegistrationRepository.findByTenantKey(tenantKey));

        //register one tenant client with full configuration
        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false, false);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        assertEquals(1, clientRegistrationRepository.findByTenantKey(tenantKey).size());
    }

    @Test
    public void test_shouldRemoveAllRegisteredClientForTenant() {
        String tenantKey = "tenant1";

        //register two clients for tenant
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 2;
        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false, false);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, false);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        //remove all clients registration for tenant

        registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, false, false, false);
        registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, false, false);

        assertNull(clientRegistrationRepository.findByTenantKey(tenantKey));
    }

    @Test
    public void test_shouldSuccessfullyRegisterOneClientPerTenant() {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 1;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true, false);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        tenantKey = "tenant2";
        clientKeyPrefix = "Auth0_";

        idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true, false);
        idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);

        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);
    }

}
