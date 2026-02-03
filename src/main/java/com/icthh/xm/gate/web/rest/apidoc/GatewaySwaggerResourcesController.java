package com.icthh.xm.gate.web.rest.apidoc;

import com.icthh.xm.gate.config.properties.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private static final String SWAGGER_V3 = "v3";

    private final DiscoveryClient discoveryClient;
    private final ApplicationProperties applicationProperties;

    @GetMapping
    public List<SwaggerResource> swaggerResources() {
        List<SwaggerResource> resources = new ArrayList<>();
        Set<String> excludedServices = applicationProperties.getGateway().getExcludedServices();

        // Add the default swagger resource that corresponds to the gateway's own swagger doc
        resources.add(createSwaggerResource("default", API_DOCS_PATH_V2, SWAGGER_VERSION_2));

        // Add the registered microservices swagger docs as additional swagger resources
        discoveryClient.getServices().stream()
            .filter(serviceId -> !excludedServices.contains(serviceId))
            .forEach(serviceId -> {
                List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
                log.debug("Service {} instances.size={}", serviceId, instances.size());

                String swaggerVersion = SWAGGER_VERSION_2;
                String apiDocsPath = API_DOCS_PATH_V2;

                if (CollectionUtils.isNotEmpty(instances)) {
                    if (SWAGGER_V3.equalsIgnoreCase(instances.get(0).getMetadata().get("swagger"))) {
                        swaggerVersion = SWAGGER_VERSION_3;
                        apiDocsPath = API_DOCS_PATH_V3;
                    }
                }
                resources.add(createSwaggerResource(serviceId, "/" + serviceId + "/" + apiDocsPath, swaggerVersion));
            });

        return resources;
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
