package com.icthh.xm.gate.service;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static com.icthh.xm.gate.config.Constants.DEFAULT_TENANT;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.icthh.xm.commons.config.domain.TenantState;
import com.icthh.xm.gate.config.ApplicationProperties;
import com.icthh.xm.gate.repository.TenantDomainRepository;
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

@Slf4j
@Component
public class TenantMappingServiceImpl implements TenantMappingService {

    private static final String IP_REGEX = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";
    public static final String TENANTS_LIST_CONFIG_KEY = "/config/tenants/tenants-list.json";
    public static final String ACTIVE = "ACTIVE";

    private final List<String> hosts;
    private final boolean redirectToDefaultTenantEnabled;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String applicationName;

    private volatile Map<String, String> tenantByDomain = new HashMap<>();
    private volatile Map<String, Set<TenantState>> tenantsByServiceMap = new HashMap<>();

    private final TenantDomainRepository tenantDomainRepository;

    public TenantMappingServiceImpl(ApplicationProperties applicationProperties,
                                    TenantDomainRepository tenantDomainRepository,
                                    @Value("${spring.application.name}") String applicationName) {
        this.redirectToDefaultTenantEnabled = TRUE.equals(applicationProperties.getRedirectToDefaultTenantEnabled());
        this.hosts = unmodifiableList(applicationProperties.getHosts());
        this.tenantDomainRepository = tenantDomainRepository;
        this.applicationName = applicationName;
    }

    @Override
    public Map<String, String> getTenantByDomain() {
        return Collections.unmodifiableMap(tenantByDomain);
    }

    @Override
    public void onRefresh(String key, String config) {
        updateTenants(config);
    }

    @Override
    public String getTenantKey(final String domain) {

        String tenantKey = Optional.ofNullable(tenantDomainRepository.getTenantKey(domain))
                                   .orElse(getTenantByDomain().get(domain));

        if (StringUtils.isBlank(tenantKey) && redirectToDefaultTenantEnabled) {
            printWarnIfNotIpAddress(domain);
            tenantKey = DEFAULT_TENANT;
        }
        return tenantKey;
    }

    private void printWarnIfNotIpAddress(String domain) {
        if (StringUtils.isNotEmpty(domain) && !domain.matches(IP_REGEX)) {
            log.warn("No mapping for domain: [{}]. default tenant applied: {}", domain, DEFAULT_TENANT);
        }
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

        this.tenantsByServiceMap = tenantsByServiceMap;
        this.tenantByDomain = tenants;
    }

    @Override
    public boolean isTenantPresent(String tenantName) {
        return tenantsByServiceMap.getOrDefault(applicationName, emptySet())
            .stream()
            .anyMatch(tenant -> tenant.getName().equalsIgnoreCase(tenantName));
    }

    @Override
    public boolean isTenantActive(String tenantName) {
        return tenantsByServiceMap.getOrDefault(applicationName, emptySet())
            .stream()
            .filter(tenant -> tenant.getName().equalsIgnoreCase(tenantName))
            .anyMatch(tenantState -> tenantState.getState().equals(ACTIVE));
    }

    @Override
    public boolean isListeningConfiguration(String updatedKey) {
        return TENANTS_LIST_CONFIG_KEY.equals(updatedKey);
    }

    @Override
    public void onInit(String key, String config) {
        updateTenants(config);
    }

}
