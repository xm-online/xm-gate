package com.icthh.xm.gate.web.rest;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import com.icthh.xm.gate.GateApp;
import com.icthh.xm.gate.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.gate.service.MonitoringService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@RunWith(SpringRunner.class)
@WithMockUser(authorities = {"SUPER-ADMIN"})
@SpringBootTest(classes = {GateApp.class, SecurityBeanOverrideConfiguration.class})
public class MonitoringResourceIntTest {

    private MockMvc mvc;

    @Autowired
    private MonitoringService service;

    @MockBean
    private ConsulClient consulClient;

    @SneakyThrows
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        MonitoringResource controller = new MonitoringResource(service);
        this.mvc = MockMvcBuilders
            .standaloneSetup(controller)
            .build();
    }

    @Test
    public void testGetServicesServices() throws Exception {

        when(consulClient.getCatalogServices(QueryParams.DEFAULT)).thenReturn(getResponse());
        when(consulClient.getHealthServices("gate", false, null))
            .thenReturn(getGateResponse());
        when(consulClient.getHealthServices("uaa", false, null))
            .thenReturn(getUaaResponse());

        MvcResult result = mvc
            .perform(get("/api/monitoring/services"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andReturn();

        log.info(result.getResponse().getContentAsString());
        verify(consulClient, times(1)).getCatalogServices(QueryParams.DEFAULT);
        verify(consulClient, times(2)).getHealthServices(anyString(), anyBoolean(), anyObject());

        verifyNoMoreInteractions(consulClient);
    }

    @Test
    public void testGetHealth() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Test
    public void testGetMetrics() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    private Response<Map<String, List<String>>> getResponse() {
        List<String> servicesList1 = new ArrayList<>();
        servicesList1.add("gate1");

        List<String> servicesList2 = new ArrayList<>();
        servicesList2.add("uaa1");

        Map<String, List<String>> value = new HashMap<>();
        value.put("gate", servicesList1);
        value.put("uaa", servicesList2);

        return new Response<>(value, 1L, true, 1L);
    }

    private Response<List<HealthService>> getGateResponse() {
        List<HealthService> value = new ArrayList<>();

        HealthService.Service service1 = new HealthService.Service();
        service1.setPort(8080);
        service1.setId("gate1");
        service1.setAddress("127.0.0.1");

        HealthService healthService1 = new HealthService();
        healthService1.setService(service1);
        healthService1.setNode(new HealthService.Node());
        value.add(healthService1);

        HealthService.Service service2 = new HealthService.Service();
        service2.setPort(8081);
        service2.setId("gate2");
        service2.setAddress("127.0.0.1");

        HealthService healthService2 = new HealthService();
        healthService2.setService(service2);
        healthService2.setNode(new HealthService.Node());
        value.add(healthService2);

        return new Response<>(value, 1L, true, 1L);
    }

    private Response<List<HealthService>> getUaaResponse() {
        List<HealthService> value = new ArrayList<>();

        HealthService.Service service1 = new HealthService.Service();
        service1.setPort(8080);
        service1.setId("uaa1");
        service1.setAddress("127.0.0.1");

        HealthService healthService1 = new HealthService();
        healthService1.setService(service1);
        healthService1.setNode(new HealthService.Node());
        value.add(healthService1);


        return new Response<>(value, 1L, true, 1L);
    }
}
