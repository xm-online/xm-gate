package com.icthh.xm.gate.web.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icthh.xm.gate.GateApp;
import com.icthh.xm.gate.config.SecurityBeanOverrideConfiguration;
import java.net.URI;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties.SimpleServiceInstance;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestGatewaySupport;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GateApp.class, SecurityBeanOverrideConfiguration.class})
public class UploadResourceTest {

    private RestTemplate notBufferRestTemplate = new RestTemplate();
    private MockRestServiceServer mockServer;
    private MockMvc mockMvc;

    @Before
    public void before() {
        RestGatewaySupport gateway = new RestGatewaySupport();
        gateway.setRestTemplate(notBufferRestTemplate);
        mockServer = MockRestServiceServer.createServer(gateway);
        ServiceInstanceChooser serviceInstanceChooser = (name) -> new SimpleServiceInstance(toUri(name));
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(new UploadResource(notBufferRestTemplate, serviceInstanceChooser))
            .build();
    }

    @SneakyThrows
    private URI toUri(String name) {
        return new URI("http://host" + name + ":7000");
    }

    @Test
    @SneakyThrows
    public void testCallUploadEndpoint() {

        mockServer.expect(once(), requestTo("http://hostentity:7000/api/functions/UPLOAD/upload"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        MockMultipartFile file = new MockMultipartFile("file", "orig", "text/plain", "test no json content" .getBytes(UTF_8));
        mockMvc.perform(multipart("/upload/entity/api/functions/UPLOAD/upload").file(file))
            .andDo(print())
            .andExpect(status().isOk());

        mockServer.verify();
    }

}
