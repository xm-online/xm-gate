package com.icthh.xm.gate.web.rest;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import com.icthh.xm.gate.web.rest.dto.MultipartFileResource;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.util.UrlPathHelper;

@Slf4j
@RestController
public class UploadResource {

    public static final String UPLOAD_PREFIX = "/upload";
    private final UrlPathHelper urlHelper = new UrlPathHelper();
    private final RestTemplate restTemplate;

    public UploadResource(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PutMapping(value = UPLOAD_PREFIX)
    @PostMapping(value = UPLOAD_PREFIX)
    public ResponseEntity<Object> upload(MultipartHttpServletRequest request) throws Exception {

        final MultiValueMap<String, Object> requestParts = new LinkedMultiValueMap<>();

        request.getParameterMap().forEach((name, value) -> requestParts.addAll(name, asList(value)));
        request.getMultiFileMap().forEach((name, value) -> {
            List<Resource> resources = value.stream().map(MultipartFileResource::new).collect(toList());
            requestParts.addAll(name, resources);
        });

        return restTemplate.exchange("http://" + extractPath(request) +  "?" + request.getQueryString(),
                                     request.getRequestMethod(),
                                     new HttpEntity<>(requestParts, request.getRequestHeaders()), Object.class);

    }

    private String extractPath(HttpServletRequest request) {
        return urlHelper.getPathWithinApplication(request).substring(UPLOAD_PREFIX.length());
    }

}
