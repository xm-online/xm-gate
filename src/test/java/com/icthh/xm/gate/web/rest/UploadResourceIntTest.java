package com.icthh.xm.gate.web.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.i18n.error.web.ExceptionTranslator;
import com.icthh.xm.gate.IntegrationTest;
import com.icthh.xm.gate.service.file.upload.UploadFileService;
import com.icthh.xm.gate.web.rest.file.UploadResource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.of;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class UploadResourceIntTest {

    private static final String SERVICE_NAME = "entity";
    private static final String SERVICE_HOST = "http://hostentity:7000";
    private static final String UPLOAD_PATH = "/api/functions/UPLOAD/upload";

    private static final String TEST_RID = "testRid";
    private static final String ERROR_CODE_TEST = "error.code.test";
    private static final String EXCEPTION_MESSAGE = "exception message";

    private MockMvc mockMvc;
    private MockRestServiceServer mockServer;

    @Mock
    private DiscoveryClient discoveryClient;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);

        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).bufferContent().build();

        RestClient restClient = restClientBuilder.build();

        ServiceInstance serviceInstance = createServiceInstance();
        when(discoveryClient.getInstances(eq(SERVICE_NAME))).thenReturn(List.of(serviceInstance));

        UploadFileService uploadFileService = new UploadFileService(restClient, discoveryClient);

        mockMvc = MockMvcBuilders
            .standaloneSetup(new UploadResource(uploadFileService))
            .setControllerAdvice(exceptionTranslator)
            .build();
    }

    @Test
    @SneakyThrows
    void testCallUploadEndpoint() {
        mockServer.expect(once(), requestTo(SERVICE_HOST + UPLOAD_PATH))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        MockMultipartFile file = new MockMultipartFile(
            "file", "orig", "text/plain", "test no json content".getBytes(UTF_8));

        mockMvc.perform(multipart("/upload/entity" + UPLOAD_PATH).file(file))
            .andDo(print())
            .andExpect(status().isOk());

        mockServer.verify();
    }

    @Test
    @SneakyThrows
    void testFailCallUploadEndpointWithHttpsInQueryParams() {
        mockServer.expect(never(), requestTo(SERVICE_HOST + UPLOAD_PATH + "?token=https://token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        MockMultipartFile file = new MockMultipartFile(
            "file", "orig", "text/plain", "test no json content".getBytes(UTF_8));

        mockMvc.perform(multipart("/upload/entity" + UPLOAD_PATH + "?token=https://token").file(file))
            .andDo(print())
            .andExpect(status().isInternalServerError());

        mockServer.verify();
    }

    @Test
    @SneakyThrows
    void testCallUploadEndpointWithQueryParams() {
        mockServer.expect(once(), requestTo(SERVICE_HOST + UPLOAD_PATH + "?token=token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        MockMultipartFile file = new MockMultipartFile(
            "file", "orig", "text/plain", "test no json content".getBytes(UTF_8));

        mockMvc.perform(multipart("/upload/entity" + UPLOAD_PATH + "?token=token").file(file))
            .andDo(print())
            .andExpect(status().isOk());

        mockServer.verify();
    }

    @Test
    @SneakyThrows
    void testCallUploadEndpointWithInternalServerError() {
        mockServer.expect(once(), requestTo(SERVICE_HOST + UPLOAD_PATH))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        mockMvc.perform(multipart("/upload/entity" + UPLOAD_PATH))
            .andDo(print())
            .andExpect(status().isInternalServerError());

        mockServer.verify();
    }

    @Test
    @SneakyThrows
    void testCallUploadEndpointWithAccessDeniedError() {
        mockServer.expect(once(), requestTo(SERVICE_HOST + UPLOAD_PATH))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.FORBIDDEN));

        mockMvc.perform(multipart("/upload/entity" + UPLOAD_PATH))
            .andDo(print())
            .andExpect(status().isForbidden());

        mockServer.verify();
    }

    @Test
    @SneakyThrows
    void testCallUploadEndpointWithBadRequest() {
        mockServer.expect(once(), requestTo(SERVICE_HOST + UPLOAD_PATH))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        mockMvc.perform(multipart("/upload/entity" + UPLOAD_PATH))
            .andDo(print())
            .andExpect(status().isBadRequest());

        mockServer.verify();
    }

    @Test
    @SneakyThrows
    public void testCallUploadEndpointWithBusinessError() {
        mockServer.expect(once(), requestTo(SERVICE_HOST + UPLOAD_PATH))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).body(getBusinessErrorBody()));

        mockMvc.perform(multipart("/upload/entity" + UPLOAD_PATH))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(ERROR_CODE_TEST))
            .andExpect(jsonPath("$.error_description").value(EXCEPTION_MESSAGE))
            .andExpect(jsonPath("$.requestId").value(TEST_RID))
            .andExpect(jsonPath("$.params.param1").value("value1"))
            .andExpect(jsonPath("$.params.param2").value("value2"));

        mockServer.verify();
    }

    private String getBusinessErrorBody() throws JsonProcessingException {
        var paramMap = of("param1", "value1", "param2", "value2");
        var bodyMap = of(
            "requestId", TEST_RID,
            "error", ERROR_CODE_TEST,
            "error_description", EXCEPTION_MESSAGE,
            "params", paramMap
        );
        return new ObjectMapper().writeValueAsString(bodyMap);
    }

    private ServiceInstance createServiceInstance() {
        return new DefaultServiceInstance(
            SERVICE_NAME + "-1",
            SERVICE_NAME,
            "host" + SERVICE_NAME,
            7000,
            false,
            Map.of()
        );
    }
}
