package com.icthh.xm.gate.web.rest;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import org.apache.commons.lang.StringUtils;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.ServiceInstanceChooser;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.UrlPathHelper;

/**
 * Endpoint for uploading multipart form.
 *
 * Example usage:
 * http://<host>:<port>/upload/entity/api/functions/FUNCTION-NAME/upload
 * This request will be proxy multipart form to entity to /api/functions/FUNCTION-NAME/upload
 *
 * Reason for create this class:
 * 1) FormBodyWrapperFilter read file to memory
 * 2) LoadBalancerRequestFactory operate with byte array (and read file to memory)
 * 3) If content length of multipart resource specified and > -1 than rest template use ByteArrayOutputStream (and read file to memory)
 *
 */
@Slf4j
@RestController
public class UploadResource {

    private static final String [] EXCLUDE_LIST = {"https://", "http://", "file://"};

    private static final String UPLOAD_PREFIX = "/upload/";
    private final UrlPathHelper urlHelper = new UrlPathHelper();
    private final RestTemplate restTemplate;
    private final ServiceInstanceChooser serviceInstanceChooser;

    public UploadResource(@Qualifier("notBufferRestTemplate") RestTemplate restTemplate,
                          ServiceInstanceChooser serviceInstanceChooser) {
        this.restTemplate = restTemplate;
        this.serviceInstanceChooser = serviceInstanceChooser;
    }

    @RequestMapping(value = UPLOAD_PREFIX + "**", method = {POST, PUT})
    public ResponseEntity<Object> upload(MultipartHttpServletRequest request) throws Exception {

        final MultiValueMap<String, Object> requestParts = new LinkedMultiValueMap<>();

        request.getParameterMap().forEach((name, value) -> requestParts.addAll(name, asList(value)));
        request.getMultiFileMap().forEach((name, value) -> {
            List<Resource> resources = value.stream().map(MultipartFileResource::new).collect(toList());
            value.forEach(file -> requestParts.add(file.getOriginalFilename() + "-" + CONTENT_LENGTH, file.getSize()));
            requestParts.addAll(name, resources);
        });

        HttpHeaders requestHeaders = request.getRequestHeaders();
        requestHeaders.setContentLength(-1L); // for avoid read request to memory in message converter
        requestHeaders.set(ACCEPT_ENCODING, "*");
        String servicePath = extractPath(request);
        return restTemplate.exchange(servicePath, request.getRequestMethod(),
                                     new HttpEntity<>(requestParts, requestHeaders), Object.class);
    }

    private String extractPath(HttpServletRequest request) {
        final String path = urlHelper.getPathWithinApplication(request).substring(UPLOAD_PREFIX.length());
        final String serviceName = path.substring(0, path.indexOf("/"));
        if (!StringUtils.isAlphanumeric(serviceName)) {
            throw new IllegalArgumentException("Service name should be alphanumeric");
        }
        final String subPath = path.substring(path.indexOf("/"));
        String query = isBlank(request.getQueryString()) ? "" : "?" + request.getQueryString();
        boolean filterPath = Arrays.stream(EXCLUDE_LIST).anyMatch(query::contains);
        if (filterPath) {
            throw new IllegalArgumentException("Query string contains reference");
        }
        ServiceInstance serviceInstance = serviceInstanceChooser.choose(serviceName);
        assertNotNull(serviceName, serviceInstance);
        return serviceInstance.getUri().toString() + subPath + query;
    }

    private void assertNotNull(String serviceName, ServiceInstance serviceInstance) {
        if (serviceInstance == null) {
            throw new IllegalStateException("No instances available for " + serviceName);
        }
    }

    static class MultipartFileResource extends InputStreamResource {

        private final String filename;

        public MultipartFileResource(MultipartFile file) {
            super(getInputStream(file));
            filename = file.getOriginalFilename();
        }

        @SneakyThrows
        private static InputStream getInputStream(MultipartFile file) {
            return file.getInputStream();
        }

        @Override
        public String getFilename() {
            return this.filename;
        }

        @Override
        public long contentLength() throws IOException {
            return -1; // for avoid read file to memory
        }
    }

}
