package com.icthh.xm.gate.web.rest;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.gate.GateApp;
import com.icthh.xm.gate.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.gate.service.MonitoringService;
import com.icthh.xm.gate.web.client.MsMonitoringClient;
import feign.Request;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
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

    @MockBean
    private MsMonitoringClient metricsClient;

    @SneakyThrows
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        MonitoringResource controller = new MonitoringResource(service);
        this.mvc = MockMvcBuilders
            .standaloneSetup(controller)
            .build();

        when(consulClient.getCatalogServices(QueryParams.DEFAULT)).thenReturn(buildServiceList());
        when(consulClient.getHealthServices("gate", false, null))
            .thenReturn(buildGateServiceResponse());
        when(consulClient.getHealthServices("uaa", false, null))
            .thenReturn(buildUaaServiceResponse());
    }

    @Test
    public void testGetServicesServices() throws Exception {
        MvcResult result = mvc
            .perform(get("/api/monitoring/services"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].name").value(containsInAnyOrder("uaa", "gate")))
            .andExpect(jsonPath("$.[*].instances[*]").isNotEmpty())
            .andExpect(jsonPath("$.[*].instances[*].id")
                .value(containsInAnyOrder("uaa1", "gate1", "gate2")))
            .andReturn();

        log.info(result.getResponse().getContentAsString());
        verify(consulClient, times(1)).getCatalogServices(QueryParams.DEFAULT);
        verify(consulClient, times(2)).getHealthServices(anyString(), anyBoolean(), any());

        verifyNoMoreInteractions(consulClient);
    }

    @Test
    public void testGetHealthActuatorV1() throws Exception {
        testGetHealth(buildHealthV1());
    }

    @Test
    public void testGetHealthActuatorV2() throws Exception {
        testGetHealth(buildHealthV2());
    }

    public void testGetHealth(Map healthResponse) throws Exception {
        feign.Response response = feign.Response.builder()
            .status(HttpStatus.OK.value())
            .headers(new HashMap<>())
            .body(new ObjectMapper().writeValueAsBytes(healthResponse))
            .request(Request.create("get", "", new HashMap<>(), null, null)).build();
        when(metricsClient.getHealth(any(), any())).thenReturn(response);

        MvcResult result = mvc
            .perform(get("/api/monitoring/services/gate/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*]").isNotEmpty())
            .andExpect(jsonPath("$.[*].health").isNotEmpty())
            .andExpect(jsonPath("$.[*].health.details").isNotEmpty())
            .andExpect(jsonPath("$.[*].health.status").value(containsInAnyOrder("UP", "UP")))
            .andExpect(jsonPath("$.[*].instanceId").value(containsInAnyOrder("gate1", "gate2")))
            .andReturn();

        log.info(result.getResponse().getContentAsString());
        verify(consulClient, times(1)).getHealthServices(anyString(), anyBoolean(), any());

        verifyNoMoreInteractions(consulClient);
    }

    @Test
    public void testGetMetrics() throws Exception {
        when(metricsClient.getMetrics(any(), any())).thenReturn(buildMetrics());

        MvcResult result = mvc
            .perform(get("/api/monitoring/services/gate/metrics"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*]").isNotEmpty())
            .andExpect(jsonPath("$.[*].metrics").isNotEmpty())
            .andExpect(jsonPath("$.[*].instanceId").value(containsInAnyOrder("gate1", "gate2")))
            .andReturn();

        log.info(result.getResponse().getContentAsString());
        verify(consulClient, times(1)).getHealthServices(anyString(), anyBoolean(), any());

        verifyNoMoreInteractions(consulClient);
    }

    private Response<Map<String, List<String>>> buildServiceList() {
        List<String> servicesList1 = new ArrayList<>();
        servicesList1.add("gate1");

        List<String> servicesList2 = new ArrayList<>();
        servicesList2.add("uaa1");

        Map<String, List<String>> value = new HashMap<>();
        value.put("gate", servicesList1);
        value.put("uaa", servicesList2);

        return new Response<>(value, 1L, true, 1L);
    }

    private Response<List<HealthService>> buildGateServiceResponse() {
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

    private Response<List<HealthService>> buildUaaServiceResponse() {
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

    private Map buildMetrics() {
        Map<Object, Object> metrics = new HashMap<>();
        metrics.put("metric1", new HashMap<Object, Object>() {{
            put("jvm.metric.value", "some-value");
        }});
        return metrics;
    }

    private Map buildHealthV2() throws IOException {
        Map<Object, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("details", new HashMap<String, Object>() {{
            put("db", "UP");
        }});
        return health;
    }

    private Map buildHealthV1() throws IOException {
        Map<Object, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("db", "UP");
        return health;
    }
}
