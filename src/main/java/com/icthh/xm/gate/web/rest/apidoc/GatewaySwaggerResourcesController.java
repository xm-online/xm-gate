package com.icthh.xm.gate.web.rest.apidoc;

import com.icthh.xm.gate.config.properties.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.consul.discovery.ConsulServiceInstance;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

/**
 * Retrieves all registered microservices Swagger resources.
 */
@RestController
@RequestMapping("/swagger-resources")
@Profile("api-docs")
@RequiredArgsConstructor
@Slf4j
public class GatewaySwaggerResourcesController {

    private static final String SWAGGER_VERSION_2 = "2.0";
    private static final String SWAGGER_VERSION_3 = "3.0";
    public static final String API_DOCS_PATH_V2 = "v2/api-docs";
    public static final String API_DOCS_PATH_V3 = "v3/api-docs";
    private static final String SWAGGER_V2_TAG = "swagger=v2";

    private final DiscoveryClient discoveryClient;
    private final ApplicationProperties applicationProperties;

    @GetMapping
    public List<SwaggerResource> swaggerResources() {
        List<SwaggerResource> resources = new ArrayList<>();
        Set<String> excludedServices = applicationProperties.getGateway().getExcludedServices();

        resources.add(createSwaggerResource("default", API_DOCS_PATH_V3, SWAGGER_VERSION_3));

        discoveryClient.getServices().stream()
            .filter(serviceId -> !excludedServices.contains(serviceId))
            .map(this::toSwaggerResource)
            .forEach(resources::add);

        return resources;
    }

    private SwaggerResource toSwaggerResource(String serviceId) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        log.debug("Service {} instances.size={}", serviceId, instances.size());

        boolean isV2 = serviceHasSwaggerV2Tag(instances);
        String swaggerVersion = isV2 ? SWAGGER_VERSION_2 : SWAGGER_VERSION_3;
        String apiDocsPath = isV2 ? API_DOCS_PATH_V2 : API_DOCS_PATH_V3;

        return createSwaggerResource(serviceId, "/" + serviceId + "/" + apiDocsPath, swaggerVersion);
    }

    private boolean serviceHasSwaggerV2Tag(List<ServiceInstance> instances) {
        return isNotEmpty(instances) && ((ConsulServiceInstance) instances.getFirst()).getTags().contains(SWAGGER_V2_TAG);
    }

    private SwaggerResource createSwaggerResource(String name, String location, String swaggerVersion) {
        SwaggerResource resource = new SwaggerResource();
        resource.setName(name);
        resource.setUrl(location);
        resource.setSwaggerVersion(swaggerVersion);
        resource.setLocation(location);
        return resource;
    }
}
