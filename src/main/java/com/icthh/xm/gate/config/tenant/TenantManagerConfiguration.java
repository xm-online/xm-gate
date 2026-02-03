package com.icthh.xm.gate.config.tenant;

import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantAbilityCheckerProvisioner;
import com.icthh.xm.commons.tenantendpoint.provisioner.TenantListProvisioner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TenantManagerConfiguration {

    @Bean
    public TenantManager tenantManager(TenantAbilityCheckerProvisioner abilityCheckerProvisioner,
                                       TenantListProvisioner tenantListProvisioner) {
        TenantManager manager = TenantManager.builder()
             .service(abilityCheckerProvisioner)
             .service(tenantListProvisioner)
             .build();
        log.info("Configured tenant manager: {}", manager);
        return manager;
    }

}
