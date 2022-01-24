package com.icthh.xm.gate.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.logging.util.MdcUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class RestTemplateErrorHandler implements ResponseErrorHandler {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new AfterburnerModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().series() != HttpStatus.Series.SUCCESSFUL;
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        String responseBody = readResponse(response.getBody());
        if (response.getStatusCode().series().equals(HttpStatus.Series.SERVER_ERROR)) {
            throw new RuntimeException(responseBody);
        } else if (response.getStatusCode().series().equals(HttpStatus.Series.CLIENT_ERROR)) {
            if (response.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
                throw new AccessDeniedException(responseBody);
            }

            BusinessDto businessDto = objectMapper.readValue(responseBody, BusinessDto.class);
            if (StringUtils.isNotBlank(businessDto.getRequestId())) {
                log.warn("Change rid from {} to {}", MdcUtils.getRid(), businessDto.getRequestId());
                MdcUtils.putRid(businessDto.getRequestId());
                throw new BusinessException(businessDto.getError(), businessDto.getErrorDescription(), businessDto.getParams());
            }
        }
    }

    private String readResponse(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int ch; (ch = inputStream.read()) != -1; ) {
            sb.append((char) ch);
        }
        return sb.toString();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessDto {
        private String requestId;
        private String error;
        @JsonProperty("error_description")
        private String errorDescription;
        private Map<String, String> params = new HashMap<>();
    }
}
