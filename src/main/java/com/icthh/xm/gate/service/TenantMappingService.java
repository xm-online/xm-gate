package com.icthh.xm.gate.service;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.gen.model.Tenant;

import java.util.Map;

public interface TenantMappingService extends RefreshableConfiguration {

    Map<String, String> getTenants();

    void addTenant(Tenant tenant);

    void deleteTenant(String tenantDomain);

    void manageTenant(String tenantDomain, String state);

    String getTenantKey(String domain);
}
