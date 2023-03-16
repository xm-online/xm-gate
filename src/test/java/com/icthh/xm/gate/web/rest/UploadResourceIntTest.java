package com.icthh.xm.gate.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.exceptions.ErrorConstants;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.gate.GateApp;
import com.icthh.xm.gate.config.RestTemplateErrorHandler.BusinessDto;
import com.icthh.xm.gate.config.SecurityBeanOverrideConfiguration;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryProperties.SimpleServiceInstance;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import static org.springframework.test.web.client.ExpectedCount.never;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestGatewaySupport;

import java.net.URI;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.of;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {GateApp.class, SecurityBeanOverrideConfiguration.class})
public class UploadResourceIntTest {

    public static final String TEST_RID = "testRid";
    public static final String ERROR_CODE_TEST = "error.code.test";
    public static final String EXCEPTION_MESSAGE = "exception message";
    @SpyBean
    @Qualifier("notBufferRestTemplate")
    private RestTemplate notBufferRestTemplate;
    private MockRestServiceServer mockServer;
    private MockMvc mockMvc;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Before
    public void before() {
        RestGatewaySupport gateway = new RestGatewaySupport();
        gateway.setRestTemplate(notBufferRestTemplate);
        mockServer = MockRestServiceServer.createServer(gateway);
        ServiceInstanceChooser serviceInstanceChooser = (name) -> new SimpleServiceInstance(toUri(name));
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(new UploadResource(notBufferRestTemplate, serviceInstanceChooser))
            .setControllerAdvice(exceptionTranslator)
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

    @Test
    @SneakyThrows
    public void testFailCallUploadEndpointWithQueryParams() {

        mockServer.expect(never(), requestTo("http://hostentity:7000/api/functions/UPLOAD/upload?token=https://token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        MockMultipartFile file = new MockMultipartFile("file", "orig", "text/plain", "test no json content" .getBytes(UTF_8));
        mockMvc.perform(multipart("/upload/entity/api/functions/UPLOAD/upload?token=https://token").file(file))
            .andDo(print())
            .andExpect(status().isInternalServerError());

        mockServer.verify();
    }

    @Test
    @SneakyThrows
    public void testCallUploadEndpointWithQueryParams() {

        mockServer.expect(once(), requestTo("http://hostentity:7000/api/functions/UPLOAD/upload?token=token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        MockMultipartFile file = new MockMultipartFile("file", "orig", "text/plain", "test no json content" .getBytes(UTF_8));
        mockMvc.perform(multipart("/upload/entity/api/functions/UPLOAD/upload?token=token").file(file))
            .andDo(print())
            .andExpect(status().isOk());

        mockServer.verify();
    }

    @Test
    @SneakyThrows
    public void testCallUploadEndpointWithInternalServerError() {

        mockServer.expect(once(), requestTo("http://hostentity:7000/api/functions/UPLOAD/upload"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(multipart("/upload/entity/api/functions/UPLOAD/upload"))
            .andDo(print())
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_INTERNAL_SERVER_ERROR))
            .andExpect(jsonPath("$.error_description").value("Internal server error, please try later"));

        mockServer.verify();
    }

    @Test
    @SneakyThrows
    public void testCallUploadEndpointWithBusinessError() {
        var map = of("param1", "value1", "param2", "value2");
        var body = new ObjectMapper().writeValueAsString(new BusinessDto(TEST_RID, ERROR_CODE_TEST, EXCEPTION_MESSAGE, map));

        mockServer.expect(once(), requestTo("http://hostentity:7000/api/functions/UPLOAD/upload"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).body(body));

        mockMvc.perform(multipart("/upload/entity/api/functions/UPLOAD/upload"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(ERROR_CODE_TEST))
            .andExpect(jsonPath("$.error_description").value(EXCEPTION_MESSAGE))
            .andExpect(jsonPath("$.requestId").value(TEST_RID))
            .andExpect(jsonPath("$.params.param1").value("value1"))
            .andExpect(jsonPath("$.params.param2").value("value2"));

        mockServer.verify();
    }

    @Test
    @SneakyThrows
    public void testCallUploadEndpointWithAccessDeniedError() {

        mockServer.expect(once(), requestTo("http://hostentity:7000/api/functions/UPLOAD/upload"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.FORBIDDEN));

        mockMvc.perform(multipart("/upload/entity/api/functions/UPLOAD/upload"))
            .andDo(print())
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value(ErrorConstants.ERR_ACCESS_DENIED))
            .andExpect(jsonPath("$.error_description").value("Access denied"));

        mockServer.verify();
    }

}
