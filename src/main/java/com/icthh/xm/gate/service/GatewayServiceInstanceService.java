package com.icthh.xm.gate.service;

import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Component
public class GatewayServiceInstanceService {

    private static final String HEALTH_CHECK_URL = "%s/management/health";
    private static final String INFO_CHECK_URL = "%s/management/info";

    private static final String STATUS_PARAM = "status";
    private static final String BUILD_PARAM = "build";

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

    private final RestClient restClient;
    private final XmAuthenticationContextHolder authContextHolder;
    private final String managementPort;

    public GatewayServiceInstanceService(XmAuthenticationContextHolder authContextHolder,
                                         @Value("${management.server.port:}") String managementPort) {
        this.authContextHolder = authContextHolder;
        this.managementPort = managementPort;
        this.restClient = RestClient.builder().build();
    }

    public Map<String, String> receiveServiceStatus(List<ServiceInstance> instances) {
        return instances.stream()
            .filter(Objects::nonNull)
            .collect(toMap(e -> e.getUri().toString(), this::receiveServiceStatus));
    }

    public String receiveServiceStatus(ServiceInstance instance) {
        String uri = managementUri(instance);
        try {
            Map<String, Object> body = restClient.get()
                .uri(format(HEALTH_CHECK_URL, uri))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

            return Optional.ofNullable(body)
                .map(b -> b.get(STATUS_PARAM))
                .map(Object::toString)
                .orElse(STATUS_DOWN);

        } catch (RestClientException e) {
            log.error("Error occurred while getting status of the microservice by URI {}", uri, e);
            return STATUS_DOWN;
        }
    }

    public Map<String, Object> extractServiceMetaData(List<ServiceInstance> serviceInstances,
                                                      Map<String, String> serviceInstancesStatus) {
        if (MapUtils.isEmpty(serviceInstancesStatus)) {
            log.error("Microservice instances has no statuses");
            return Map.of();
        }

        return serviceInstances.stream()
            .filter(Objects::nonNull)
            .filter(instance -> StringUtils.isNotBlank(instance.getUri().toString()))
            .filter(instance -> STATUS_UP.equals(serviceInstancesStatus.get(instance.getUri().toString())))
            .collect(toMap(instance -> instance.getUri().toString(), instance -> getInstanceInfo(managementUri(instance))));
    }

    private String managementUri(ServiceInstance instance) {
        URI uri = instance.getUri();
        if (StringUtils.isBlank(managementPort)) {
            return uri.toString();
        }
        return UriComponentsBuilder.fromUri(uri).port(managementPort).toUriString();
    }

    private Map getInstanceInfo(String uri) {
        XmAuthenticationContext authContext = authContextHolder.getContext();
        Optional<String> tokenValue = authContext.getTokenValue();
        Optional<String> tokenType = authContext.getTokenType();
        if (tokenValue.isEmpty() || tokenType.isEmpty()) {
            throw new IllegalStateException("Authentication not initialized yet, can't create request");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", tokenType.get() + " " + tokenValue.get());

        try {
            Map<String, Object> body = restClient.get()
                .uri(format(INFO_CHECK_URL, uri))
                .headers(h -> h.addAll(headers))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

            return (Map) body.get(BUILD_PARAM);

        } catch (RestClientException e) {
            log.error("Error occurred while getting metadata of the microservice by URI {}", uri, e);
            return Map.of();
        }
    }
}
