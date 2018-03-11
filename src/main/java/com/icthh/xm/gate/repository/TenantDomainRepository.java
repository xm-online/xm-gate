package com.icthh.xm.gate.repository;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.icthh.xm.commons.config.client.api.RefreshableConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MS-Config based repository to store custom domain to tenant mapping in file: TENANTS_DOMAINS_CONFIG_KEY.
 */
@Slf4j
@Component
public class TenantDomainRepository implements RefreshableConfiguration {

    public static final String TENANTS_DOMAINS_CONFIG_KEY = "/config/tenants/tenant-domains.yml";

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    private final Map<String, String> tenantToDomain = new ConcurrentHashMap<>();

    public String getTenantKey(String domain) {
        return tenantToDomain.get(domain);
    }

    @Override
    public void onRefresh(final String updatedKey, final String config) {
        updateTenantsDomains(updatedKey, config);
    }

    @Override
    public boolean isListeningConfiguration(final String updatedKey) {
        return TENANTS_DOMAINS_CONFIG_KEY.equals(updatedKey);
    }

    @Override
    public void onInit(final String configKey, final String configValue) {
        updateTenantsDomains(configKey, configValue);
    }

    private void updateTenantsDomains(String configKey, String tenantDmains) {
        try {

            if (StringUtils.isBlank(tenantDmains)) {
                clearConfig();
            } else {
                CollectionType listType = defaultInstance().constructCollectionType(LinkedList.class, String.class);
                MapType mapType = defaultInstance().constructMapType(HashMap.class,
                                                                     defaultInstance().constructType(String.class),
                                                                     listType);

                Map<String, List<String>> domains = objectMapper.readValue(tenantDmains, mapType);

                log.info("received tenant domain config [{}]: {}", configKey, domains);

                if (domains != null) {
                    Map<String, String> map = domains.entrySet().stream()
                                                     .flatMap(TenantDomainRepository::toPairStream)
                                                     .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
                    log.info("converted tenant domain config [{}]: {}", configKey, map);
                    tenantToDomain.clear();
                    tenantToDomain.putAll(map);
                } else {
                    clearConfig();
                }

            }

        } catch (Exception e) {
            log.error("can not update config: [{}]", configKey, e);
        }
    }

    private void clearConfig() {
        tenantToDomain.clear();
        log.info("clear tenant to domain mapping as config was deleted or empty");
    }

    private static Stream<Pair<String, String>> toPairStream(Map.Entry<String, List<String>> entry) {
        return entry.getValue().stream()
                    .map(domain -> Pair.of(StringUtils.lowerCase(domain), StringUtils.upperCase(entry.getKey())));
    }

}
