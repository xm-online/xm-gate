package com.icthh.xm.gate.web.rest;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.gate.IntegrationTest;
import com.icthh.xm.gate.repository.TenantDomainRepository;
import com.icthh.xm.gate.service.TenantMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@IntegrationTest
public class TenantDomainsMappingIntTest {

    @Autowired
    private TenantMappingService service;

    @Autowired
    private TenantDomainRepository tenantDomainRepository;

    @BeforeEach
    public void setup() {
        String domainConfig = """
            ---
            xm:
             - "test.com"
             - "test.com"
             - "localhost"
            tenant1:
             - "tenant1.COM"
             - "dev.tenant1.com"
            """;

        String configValue = """
            {
              "gate":[
                {
                  "name":"tenant2",
                  "state":"ACTIVE"
                },
                {
                  "name":"tenant3",
                  "state":"SUSPENDED"
                }
              ]
            }
            """;
        tenantDomainRepository.onInit(TenantDomainRepository.TENANTS_DOMAINS_CONFIG_KEY, domainConfig);
        service.onInit(TenantListRepository.TENANTS_LIST_CONFIG_KEY, configValue);
    }

    @Test
    public void test() {
        assertNull(service.getTenantKey("unknown"));
        assertTrue(service.isTenantPresent("tenant2"));
        assertTrue(service.isTenantPresent("tenant3"));
        assertTrue(service.isTenantPresent("TENANT3"));
        assertFalse(service.isTenantPresent("tenant4"));
        assertFalse(service.isTenantPresent(null));

        assertTrue(service.isTenantActive("tenant2"));
        assertTrue(service.isTenantActive("TENANT2"));
        assertFalse(service.isTenantActive("tenant3"));
        assertFalse(service.isTenantActive("TENANT3"));
        assertFalse(service.isTenantActive("tenant4"));
        assertFalse(service.isTenantActive(null));
    }
}
