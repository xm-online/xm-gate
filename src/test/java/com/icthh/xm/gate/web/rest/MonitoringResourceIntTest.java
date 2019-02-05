package com.icthh.xm.gate.web.rest;

import static com.icthh.xm.gate.service.TenantMappingServiceImpl.TENANTS_LIST_CONFIG_KEY;
import static io.advantageous.boon.core.Maps.map;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.config.client.repository.TenantListRepository;
import com.icthh.xm.commons.config.domain.TenantState;
import com.icthh.xm.commons.gen.api.TenantsApi;
import com.icthh.xm.commons.gen.api.TenantsApiController;
import com.icthh.xm.commons.gen.model.Tenant;
import com.icthh.xm.gate.GateApp;
import com.icthh.xm.gate.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.gate.service.TenantMappingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GateApp.class, SecurityBeanOverrideConfiguration.class})
public class TenantsApiIntTest {

    private MockMvc mvc;

    @Autowired
    private TenantMappingService service;

    Set<TenantState> tenants = new HashSet<>();

    private ObjectMapper om = new ObjectMapper();

    @Value("${spring.application.name}")
    String applicationName;

    @Autowired
    TenantListRepository tenantListRepository;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        TenantsApi controller = new TenantsApiController(new TenantResource(service));
        this.mvc = MockMvcBuilders
            .standaloneSetup(controller)
            .build();

        OAuth2Authentication auth = Mockito.mock(OAuth2Authentication.class);
        OAuth2AuthenticationDetails details = Mockito.mock(OAuth2AuthenticationDetails.class);
        when(auth.getDetails()).thenReturn(details);
        when(details.getTokenValue()).thenReturn("token");
        SecurityContextHolder.getContext().setAuthentication(auth);
        doAnswer(ans -> {
            tenants.add(new TenantState(ans.getArguments()[0].toString(), ""));
            String content = om.writeValueAsString(map(applicationName, tenants));
            service.onRefresh(TENANTS_LIST_CONFIG_KEY, content);
            return null;
        }).when(tenantListRepository).addTenant(any());
        doAnswer(ans -> {
            tenants.remove(new TenantState(ans.getArguments()[0].toString(), ""));
            String content = om.writeValueAsString(map(applicationName, tenants));
            service.onRefresh(TENANTS_LIST_CONFIG_KEY, content);
            return null;
        }).when(tenantListRepository).deleteTenant(any());
        when(tenantListRepository.getTenants()).thenReturn(tenants.stream().map(TenantState::getName)
            .collect(Collectors.toSet()));
    }


    @Test
    public void testAddTenant() throws Exception {
        ObjectMapper om = new ObjectMapper();
        mvc.perform(post("/tenants").content(om.writeValueAsBytes(new Tenant().tenantKey("testAdd"))).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        assertTrue(service.getTenants().containsKey("testadd.local"));
        assertEquals(service.getTenants().get("testadd.local"), "TESTADD");
    }

    @Test
    public void testDeleteTenant() throws Exception {
        testAddTenant();
        mvc.perform(delete("/tenants/testadd")).andExpect(status().isOk());
        assertFalse(service.getTenants().containsKey("testadd.local"));
    }

}
