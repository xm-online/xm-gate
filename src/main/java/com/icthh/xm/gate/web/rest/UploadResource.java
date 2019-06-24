package com.icthh.xm.gate.web.rest;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.io.IOException;
import java.io.InputStream;
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

@Slf4j
@RestController
public class UploadResource {

    private static final String UPLOAD_PREFIX = "/upload/";
    private final UrlPathHelper urlHelper = new UrlPathHelper();
    private final RestTemplate restTemplate;
    private final ServiceInstanceChooser serviceInstanceChooser;

    public UploadResource(@Qualifier("simpleRestTemplate") RestTemplate restTemplate,
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

        return restTemplate.exchange(extractPath(request), request.getRequestMethod(),
                                     new HttpEntity<>(requestParts, requestHeaders), Object.class);
    }

    private String extractPath(HttpServletRequest request) {
        String path = urlHelper.getPathWithinApplication(request).substring(UPLOAD_PREFIX.length());
        String serviceName = path.substring(0, path.indexOf("/"));
        path = path.substring(path.indexOf("/"));
        String query = isBlank(request.getQueryString()) ? "" : "?" + request.getQueryString();
        ServiceInstance serviceInstance = serviceInstanceChooser.choose(serviceName);
        assertNotNull(serviceName, serviceInstance);
        return serviceInstance.getUri().toString() + path + query;
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
