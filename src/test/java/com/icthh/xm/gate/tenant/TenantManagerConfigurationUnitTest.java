package com.icthh.xm.gate.tenant;

import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantAbilityCheckerProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantListProvisioner;
import com.icthh.xm.gate.config.tenant.TenantManagerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class TenantManagerConfigurationUnitTest {

    private TenantManager tenantManager;

    @Spy
    private TenantManagerConfiguration configuration = new TenantManagerConfiguration();

    @Mock
    private TenantAbilityCheckerProvisioner abilityCheckerProvisioner;
    @Mock
    private TenantListProvisioner tenantListProvisioner;

    @BeforeEach
    void setup() {
        tenantManager = configuration.tenantManager(abilityCheckerProvisioner,
                                                    tenantListProvisioner);
    }

    @Test
    void testCreateTenantProvisioningOrder() {

        tenantManager.createTenant(new Tenant().tenantKey("newtenant"));

        InOrder inOrder = Mockito.inOrder(abilityCheckerProvisioner, tenantListProvisioner);

        inOrder.verify(abilityCheckerProvisioner).createTenant(any(Tenant.class));
        inOrder.verify(tenantListProvisioner).createTenant(any(Tenant.class));

        verifyNoMoreInteractions(abilityCheckerProvisioner, tenantListProvisioner);
    }
}
