package com.icthh.xm.gate.web.rest;

import static org.junit.Assert.assertEquals;

import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.gate.GateApp;
import com.icthh.xm.gate.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.gate.repository.TenantDomainRepository;
import com.icthh.xm.gate.service.TenantMappingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GateApp.class, SecurityBeanOverrideConfiguration.class})
public class TenantDomainsMappingUnitTest {

    @Autowired
    private TenantMappingService service;

    @Autowired
    private TenantDomainRepository tenantDomainRepository;

    @Before
    public void setup() {

        String domainConfig = "---\n"
                              + "xm:\n"
                              + " - \"test.com\"\n"
                              + " - \"bla.bla.com\"\n"
                              + " - \"localhost\"\n"
                              + "tenant1:\n"
                              + " - \"tenant1.COM\"\n"
                              + " - \"dev.tenant1.com\"\n";

        tenantDomainRepository.onInit(TenantDomainRepository.TENANTS_DOMAINS_CONFIG_KEY, domainConfig);
        service.onInit(TenantListRepository.TENANTS_LIST_CONFIG_KEY,
                       "{\"gate\":[{\"name\":\"tenant2\", \"state\":\"ACTIVE\"}]}");
    }

    @Test
    public void test() {

        // custom mapping based on tenant-domains.yml
        assertEquals("XM", service.getTenantKey("bla.bla.com"));
        assertEquals("TENANT1", service.getTenantKey("tenant1.com"));
        assertEquals("TENANT1", service.getTenantKey("dev.tenant1.com"));

        // general matting based on prefixed application.hosts config
        assertEquals("TENANT2", service.getTenantKey("tenant2.local"));

        // fallback to defaul mapping
        assertEquals("XM", service.getTenantKey("unknown"));

    }

}
