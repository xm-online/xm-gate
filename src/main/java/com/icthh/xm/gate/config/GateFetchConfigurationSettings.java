package com.icthh.xm.gate.config;

import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Primary
@Component
public class GateFetchConfigurationSettings extends FetchConfigurationSettings {

    private final List<String> msConfigPatterns;
    private final Boolean isFetchAll;

    public GateFetchConfigurationSettings(@Value("${spring.application.name}") String applicationName,
                                          @Value("${application.config-fetch-all.enabled:false}") Boolean isFetchAll) {
        super(applicationName, isFetchAll);
        this.msConfigPatterns = List.of(
                "/config/tenants/commons/**",
                "/config/tenants/*",
                "/config/tenants/{tenantName}/commons/**",
                "/config/tenants/{tenantName}/*",
                "/config/tenants/{tenantName}/" + applicationName + "/**",
                "/config/tenants/{tenantName}/webapp/public/idp-config-public.yml",
                "/config/tenants/{tenantName}/config/**");
        this.isFetchAll = isFetchAll;
    }

    @Override
    public List<String> getMsConfigPatterns() {
        return msConfigPatterns;
    }

    @Override
    public Boolean getIsFetchAll() {
        return isFetchAll;
    }
}
