package com.icthh.xm.gate.web.rest;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GateApp.class, SecurityBeanOverrideConfiguration.class})
public class TenantDomainsMappingIntTest {

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
                       "{\"gate\":[{\"name\":\"tenant2\", \"state\":\"ACTIVE\"}, {\"name\":\"tenant3\", \"state\":\"SUSPENDED\"}]}");
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
