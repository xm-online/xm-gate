package com.icthh.xm.gate.web.rest;

import com.ecwid.consul.v1.ConsulClient;
import com.icthh.xm.gate.GateApp;
import com.icthh.xm.gate.config.SecurityBeanOverrideConfiguration;
import com.icthh.xm.gate.service.MonitoringService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GateApp.class, SecurityBeanOverrideConfiguration.class})
public class MonitoringResourceIntTest {

    private MockMvc mvc;

    @Autowired
    private MonitoringService service;

    @Mock
    private ConsulClient consulClient;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        MonitoringResource controller = new MonitoringResource(service);
        this.mvc = MockMvcBuilders
            .standaloneSetup(controller)
            .build();
    }

    @Test
    public void testGetServices() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Test
    public void testGetHealth() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Test
    public void testGetMetrics() throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }
}
