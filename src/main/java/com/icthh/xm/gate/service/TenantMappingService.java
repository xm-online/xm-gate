package com.icthh.xm.gate.service;

import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;

import java.util.Map;

public interface TenantMappingService extends RefreshableConfiguration {

    Map<String, String> getTenantByDomain();

    String getTenantKey(String domain);
}
