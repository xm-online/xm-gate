package com.icthh.xm.gate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.gate.config.ApplicationProperties;
import com.icthh.xm.gate.domain.properties.TenantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantPropertiesService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";

    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final ConcurrentHashMap<String, TenantProperties> tenantProps = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();

    public TenantProperties getTenantProps() {
        String tenantKey = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        String cfgTenantKey = tenantKey.toUpperCase();
        if (!tenantProps.containsKey(cfgTenantKey)) {
            throw new IllegalArgumentException("Tenant '" + cfgTenantKey + "' - configuration is empty");
        }
        return tenantProps.get(cfgTenantKey);
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        String specificationPathPattern = applicationProperties.getTenantPropertiesPathPattern();
        try {
            // tenant key in upper case
            String tenant = matcher.extractUriTemplateVariables(specificationPathPattern, updatedKey).get(TENANT_NAME);
            if (StringUtils.isBlank(config)) {
                tenantProps.remove(tenant);
                log.info("Specification for tenant {} was removed", tenant);
            } else {
                TenantProperties spec = mapper.readValue(config, TenantProperties.class);
                tenantProps.put(tenant, spec);
                log.info("Specification for tenant {} was updated", tenant);
            }
        } catch (Exception e) {
            log.error("Error read xm specification from path " + updatedKey, e);
        }
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        String specificationPathPattern = applicationProperties.getTenantPropertiesPathPattern();
        return matcher.match(specificationPathPattern, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        if (isListeningConfiguration(configKey)) {
            onRefresh(configKey, configValue);
        }
    }
}
