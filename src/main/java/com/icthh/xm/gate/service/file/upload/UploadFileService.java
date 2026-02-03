package com.icthh.xm.gate.service.file.upload;

import com.icthh.xm.commons.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.UrlPathHelper;

import java.util.Arrays;
import java.util.List;

import static com.icthh.xm.gate.config.Constants.UPLOAD_PREFIX;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isAlphanumeric;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadFileService {

    private static final String [] EXCLUDE_LIST = {"https://", "http://", "file://"};

    private final UrlPathHelper urlHelper = new UrlPathHelper();
    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;

    public ResponseEntity<Object> upload(MultipartHttpServletRequest request) {
        return restClient
            .method(request.getRequestMethod())
            .uri(extractPath(request))
            .headers(headers -> headers.addAll(getUploadHeaders(request)))
            .body(buildRequestBody(request))
            .exchange((req, res) -> ResponseEntity
                .status(res.getStatusCode())
                .headers(res.getHeaders())
                .body(res.bodyTo(byte[].class))
            );
    }

    private MultiValueMap<String, Object> buildRequestBody(MultipartHttpServletRequest request) {
        final MultiValueMap<String, Object> requestParts = new LinkedMultiValueMap<>();

        request.getParameterMap().forEach((name, value) -> requestParts.addAll(name, asList(value)));

        request.getMultiFileMap().forEach((name, value) -> {
            List<Resource> resources = value.stream().map(MultipartFileResource::new).collect(toList());
            value.forEach(file -> requestParts.add(file.getOriginalFilename() + "-" + CONTENT_LENGTH, file.getSize()));
            requestParts.addAll(name, resources);
        });
        return requestParts;
    }

    private String extractPath(HttpServletRequest request) {
        final String path = urlHelper.getPathWithinApplication(request).substring(UPLOAD_PREFIX.length());
        final String serviceName = path.substring(0, path.indexOf("/"));
        if (!isAlphanumeric(serviceName)) {
            throw new IllegalArgumentException("Service name should be alphanumeric");
        }
        final String subPath = path.substring(path.indexOf("/"));
        String query = isBlank(request.getQueryString()) ? "" : "?" + request.getQueryString();
        boolean filterPath = Arrays.stream(EXCLUDE_LIST).anyMatch(query::contains);
        if (filterPath) {
            throw new IllegalArgumentException("Query string contains reference");
        }
        ServiceInstance serviceInstance = discoveryClient.getInstances(serviceName).getFirst();
        assertNotNull(serviceName, serviceInstance);
        return serviceInstance.getUri() + subPath + query;
    }

    private void assertNotNull(String serviceName, ServiceInstance serviceInstance) {
        if (serviceInstance == null) {
            throw new IllegalStateException("No instances available for " + serviceName);
        }
    }

    private HttpHeaders getUploadHeaders(MultipartHttpServletRequest request) {
        HttpHeaders requestHeaders = request.getRequestHeaders();
        requestHeaders.remove(CONTENT_LENGTH); // remove to let RestClient calculate it or use chunked encoding
        requestHeaders.set(ACCEPT_ENCODING, "*");
        return requestHeaders;
    }
}
