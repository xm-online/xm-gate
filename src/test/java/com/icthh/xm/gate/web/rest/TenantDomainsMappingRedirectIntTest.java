package com.icthh.xm.gate.web.rest;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.gate.IntegrationTest;
import com.icthh.xm.gate.repository.TenantDomainRepository;
import com.icthh.xm.gate.service.TenantMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@IntegrationTest
@TestPropertySource(properties = "application.redirect-to-default-tenant-enabled=true")
public class TenantDomainsMappingRedirectIntTest {

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
                }
              ]
            }
            """;
        tenantDomainRepository.onInit(TenantDomainRepository.TENANTS_DOMAINS_CONFIG_KEY, domainConfig);
        service.onInit(TenantListRepository.TENANTS_LIST_CONFIG_KEY, configValue);
    }

    @Test
    public void test() {
        // custom mapping based on tenant-domains.yml
        assertEquals("XM", service.getTenantKey("bla.bla.com"));
        assertEquals("TENANT1", service.getTenantKey("tenant1.com"));
        assertEquals("TENANT1", service.getTenantKey("dev.tenant1.com"));

        // general matting based on prefixed application.hosts config
        assertEquals("TENANT2", service.getTenantKey("tenant2.local"));

        // fallback to default mapping
        assertEquals("XM", service.getTenantKey("unknown"));
    }
}
