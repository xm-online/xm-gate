package com.icthh.xm.gate.config;

import com.icthh.xm.commons.exceptions.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.io.InputStream;

@Component
public class RestTemplateErrorHandler implements ResponseErrorHandler{

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().series() != HttpStatus.Series.SUCCESSFUL;
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        if (response.getStatusCode().series().equals(HttpStatus.Series.SERVER_ERROR)) {
            throw new RuntimeException(readResponse(response.getBody()));
        } else if (response.getStatusCode().series().equals(HttpStatus.Series.CLIENT_ERROR)) {
            if (response.getStatusCode().equals(HttpStatus.FORBIDDEN)) {
                throw new AccessDeniedException(readResponse(response.getBody()));
            }
            throw new BusinessException(readResponse(response.getBody()));
        }
    }


    private String readResponse(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int ch; (ch = inputStream.read()) != -1; ) {
            sb.append((char) ch);
        }
        return sb.toString();
    }
}
