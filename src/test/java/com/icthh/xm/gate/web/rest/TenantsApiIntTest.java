package com.icthh.xm.gate.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.domain.TenantState;
import com.icthh.xm.commons.gen.api.TenantsApiController;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenantendpoint.TenantManager;
import com.icthh.xm.gate.IntegrationTest;
import com.icthh.xm.gate.service.TenantMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class TenantsApiIntTest {

    private static final String TENANTS_LIST_CONFIG_KEY = "/config/tenants/tenants-list.json";

    private MockMvc mvc;

    @Autowired
    private TenantMappingService service;

    @Autowired
    private TenantManager tenantManager;

    @Autowired
    private TenantContextHolder tenantContextHolder;

    @Autowired
    private TenantListRepository tenantListRepository;

    @Value("${spring.application.name}")
    private String applicationName;

    private final Set<TenantState> tenants = new HashSet<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tenants.clear();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
        TenantContextUtils.setTenant(tenantContextHolder, "XM");

        setupTenantListRepositoryMocks();

        mvc = MockMvcBuilders
            .standaloneSetup(new TenantsApiController(new TenantResource(tenantManager)))
            .build();
    }

    @Test
    void shouldAddTenant() throws Exception {
        Tenant tenant = new Tenant().tenantKey("testAdd");

        mvc.perform(post("/api/tenants")
                .content(objectMapper.writeValueAsBytes(tenant))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        assertTrue(service.getTenantByDomain().containsKey("testadd.local"));
        assertEquals("TESTADD", service.getTenantByDomain().get("testadd.local"));
    }

    @Test
    void shouldDeleteTenant() throws Exception {
        Tenant tenant = new Tenant().tenantKey("testDelete");
        mvc.perform(post("/api/tenants")
                .content(objectMapper.writeValueAsBytes(tenant))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        assertTrue(service.getTenantByDomain().containsKey("testdelete.local"));

        mvc.perform(delete("/api/tenants/testdelete"))
            .andExpect(status().isOk());

        assertFalse(service.getTenantByDomain().containsKey("testdelete.local"));
    }

    private void setupTenantListRepositoryMocks() {
        doAnswer(invocation -> {
            String tenantKey = invocation.getArgument(0, String.class).toLowerCase();
            tenants.add(new TenantState(tenantKey, "ACTIVE"));
            refreshTenantMappingService();
            return null;
        }).when(tenantListRepository).addTenant(any());

        doAnswer(invocation -> {
            String tenantKey = invocation.getArgument(0, String.class).toLowerCase();
            tenants.removeIf(t -> t.getName().equalsIgnoreCase(tenantKey));
            refreshTenantMappingService();
            return null;
        }).when(tenantListRepository).deleteTenant(any());

        when(tenantListRepository.getTenants())
            .thenAnswer(inv -> tenants.stream().map(TenantState::getName).collect(Collectors.toSet()));
    }

    private void refreshTenantMappingService() {
        try {
            Map<String, List<Map<String, String>>> tenantsMap = Map.of(
                applicationName,
                tenants.stream()
                    .map(t -> Map.of("name", t.getName(), "state", "ACTIVE"))
                    .toList()
            );
            String content = objectMapper.writeValueAsString(tenantsMap);
            service.onRefresh(TENANTS_LIST_CONFIG_KEY, content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh tenant mapping", e);
        }
    }
}
