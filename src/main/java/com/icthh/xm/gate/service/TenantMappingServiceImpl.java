package com.icthh.xm.gate.service;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static com.icthh.xm.gate.config.Constants.DEFAULT_TENANT;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.domain.TenantState;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.gate.config.ApplicationProperties;
import com.icthh.xm.gate.repository.TenantDomainRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class TenantMappingServiceImpl implements TenantMappingService {

    public static final String TENANTS_LIST_CONFIG_KEY = "/config/tenants/tenants-list.json";

    private final List<String> hosts = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String applicationName;

    private volatile Map<String, String> tenants = new HashMap<>();

    private final TenantListRepository tenantListRepository;
    private final TenantDomainRepository tenantDomainRepository;

    public TenantMappingServiceImpl(ApplicationProperties applicationProperties,
                                    TenantListRepository tenantListRepository,
                                    TenantDomainRepository tenantDomainRepository,
                                    @Value("${spring.application.name}") String applicationName) {
        this.hosts = unmodifiableList(applicationProperties.getHosts());
        this.tenantListRepository = tenantListRepository;
        this.tenantDomainRepository = tenantDomainRepository;
        this.applicationName = applicationName;
    }

    @Override
    public Map<String, String> getTenants() {
        return tenants;
    }

    @Override
    @SneakyThrows
    public void addTenant(Tenant tenant) {
        tenantListRepository.addTenant(tenant.getTenantKey().toLowerCase());
    }

    @Override
    @SneakyThrows
    public void deleteTenant(String tenantDomain) {
        tenantListRepository.deleteTenant(tenantDomain);
    }

    @Override
    public void onRefresh(String key, String config) {
        updateTenants(config);
    }

    @Override
    @SneakyThrows
    public void manageTenant(String tenantDomain, String state) {
        tenantListRepository.updateTenant(tenantDomain, state);
    }

    @Override
    public String getTenantKey(final String domain) {

        String tenantKey = Optional.ofNullable(tenantDomainRepository.getTenantKey(domain))
                                   .orElse(getTenants().get(domain));

        if (StringUtils.isBlank(tenantKey)) {
            log.warn("No mapping for domain: [{}]. default tenant applied: {}", domain, DEFAULT_TENANT);
            tenantKey = DEFAULT_TENANT;
        }
        return tenantKey;
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
                tenants.put(tenant.getName() + "." + host, tenant.getName().toUpperCase());
            }
        }

        this.tenants = tenants;
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
