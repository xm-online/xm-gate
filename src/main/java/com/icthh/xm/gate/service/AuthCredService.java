package com.icthh.xm.gate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.gate.config.ApplicationProperties;
import com.icthh.xm.gate.domain.TenantConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AuthCredService implements RefreshableConfiguration {

    private static final String TENANT_NAME = "tenantName";
    private final ApplicationProperties applicationProperties;
    private final TenantContextHolder tenantContextHolder;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private final ConcurrentHashMap<String, TenantConfig> tenantProps = new ConcurrentHashMap<>();
    private final AntPathMatcher matcher = new AntPathMatcher();


    public AuthCredService(ApplicationProperties appProps, TenantContextHolder tch) {
        this.applicationProperties = appProps;
        this.tenantContextHolder = tch;
    }

    public TenantConfig getTenantProps() {
        String tenantKey = TenantContextUtils.getRequiredTenantKeyValue(tenantContextHolder);
        String cfgTenantKey = tenantKey.toUpperCase();
        if (!tenantProps.containsKey(cfgTenantKey)) {
            log.info("Tenant '" + cfgTenantKey + "' - configuration is empty");
            return null;
        }
        return tenantProps.get(cfgTenantKey);
    }



    @Override
    public void onRefresh(String updatedKey, String config) {
        String specificationPathPattern = applicationProperties.getTenantPath();
        try {
            String tenant = matcher.extractUriTemplateVariables(specificationPathPattern, updatedKey).get(TENANT_NAME);
            if (StringUtils.isBlank(config)) {
                tenantProps.remove(tenant);
                log.info("Specification for tenant {} was removed", tenant);
            } else {
                TenantConfig spec = mapper.readValue(config, TenantConfig.class);
                tenantProps.put(tenant, spec);
                log.info("Specification for tenant {} was updated", tenant);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        String specificationPathPattern = applicationProperties.getTenantPath();
        return matcher.match(specificationPathPattern, updatedKey);
    }

    @Override
    public void onInit(String configKey, String configValue) {
        onRefresh(configKey, configValue);
    }
}
