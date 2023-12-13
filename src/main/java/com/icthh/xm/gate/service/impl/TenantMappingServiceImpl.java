package com.icthh.xm.gate.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.icthh.xm.commons.config.domain.TenantState;
import com.icthh.xm.gate.config.ApplicationProperties;
import com.icthh.xm.gate.repository.TenantDomainRepository;
import com.icthh.xm.gate.service.TenantMappingService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static com.icthh.xm.gate.config.Constants.DEFAULT_TENANT;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;

@Slf4j
@Component
public class TenantMappingServiceImpl implements TenantMappingService {

    private static final String IP_REGEX = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";

    private final List<String> hosts;
    private final ObjectMapper objectMapper;
    private final String applicationName;
    private volatile Map<String, String> tenantByDomain;
    private final TenantDomainRepository tenantDomainRepository;
    private final ApplicationProperties applicationProperties;

    public TenantMappingServiceImpl(ApplicationProperties applicationProperties,
                                    TenantDomainRepository tenantDomainRepository,
                                    @Value("${spring.application.name}") String applicationName) {
        this.applicationProperties = applicationProperties;
        this.hosts = unmodifiableList(applicationProperties.getHosts());
        this.tenantDomainRepository = tenantDomainRepository;
        this.applicationName = applicationName;
        this.objectMapper = new ObjectMapper();
        this.tenantByDomain = new HashMap<>();
    }

    @Override
    public Map<String, String> getTenantByDomain() {
        return Collections.unmodifiableMap(tenantByDomain);
    }

    @Override
    public String getTenantKey(String domain) {
        String tenantKey = Optional.ofNullable(tenantDomainRepository.getTenantKey(domain))
            .orElse(getTenantByDomain().get(domain));

        if (StringUtils.isBlank(tenantKey)) {
            printWarnIfNotIpAddress(domain);
            tenantKey = DEFAULT_TENANT;
        }
        return tenantKey;
    }

    @Override
    public void onRefresh(String updatedKey, String config) {
        updateTenants(config);
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return applicationProperties.getTenantPropertiesListConfigKey().equals(updatedKey);
    }

    @Override
    public void onInit(String configKey, String config) {
        updateTenants(config);
    }

    @SneakyThrows
    private void updateTenants(String config)  {
        log.info("Tenants list was updated");

        CollectionType setType = defaultInstance().constructCollectionType(HashSet.class, TenantState.class);
        MapType type = defaultInstance().constructMapType(HashMap.class, defaultInstance().constructType(String.class), setType);
        Map<String, Set<TenantState>> tenantsByServiceMap = objectMapper.readValue(config, type);

        final Map<String, String> tenants = new HashMap<>();
        for (TenantState tenant: tenantsByServiceMap.getOrDefault(applicationName, emptySet())) {
            for (String host : hosts) {
                tenants.put(tenant.getName().toLowerCase() + "." + host, tenant.getName().toUpperCase());
            }
        }

        log.info("Tenants sub-domain mapping configured by $application.hosts: {}", hosts);

        this.tenantByDomain = tenants;
    }

    private void printWarnIfNotIpAddress(String domain) {
        if (StringUtils.isNotEmpty(domain) && !domain.matches(IP_REGEX)) {
            log.warn("No mapping for domain: [{}]. default tenant applied: {}", domain, DEFAULT_TENANT);
        }
    }
}
