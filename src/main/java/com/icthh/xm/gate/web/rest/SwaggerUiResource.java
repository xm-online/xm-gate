package com.icthh.xm.gate.web.rest;

import com.icthh.xm.gate.utils.RouteUtils;
import com.icthh.xm.gate.web.swagger.SwaggerResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.config.JHipsterConstants;

import java.util.ArrayList;
import java.util.List;

@Primary
@Profile(JHipsterConstants.SPRING_PROFILE_API_DOCS)
@RequiredArgsConstructor
@Slf4j
@RestController
public class SwaggerUiResource {

    private static final String swaggerVersion2 = "2.0";
    private static final String swaggerVersion3 = "3.0";
    private static final String apiDocsPath2 = "v2/api-docs";
    private static final String apiDocsPath3 = "v3/api-docs";
    private static final String SWAGGER_V3 = "v3";

    private final RouteLocator routeLocator;

    private final DiscoveryClient discoveryClient;

    @GetMapping("/swagger-resources")
    public List<SwaggerResource> swaggerConfig() {
        List<SwaggerResource> resources = new ArrayList<>();

        //Add the default swagger resource that correspond to the gateway's own swagger doc
        resources.add(new SwaggerResource("default", apiDocsPath3, swaggerVersion3, apiDocsPath3));

        //Add the registered microservices swagger docs as additional swagger resources
        routeLocator.getRoutes().subscribe(route -> {
            String routeId = RouteUtils.clearRouteId(route.getId());
            List<ServiceInstance> instances = discoveryClient.getInstances(routeId);
            log.debug("route {} instances.size={}", routeId, instances.size());

            String swaggerVersion = swaggerVersion2;
            String apiDocsPath = apiDocsPath2;
            if (CollectionUtils.isNotEmpty(instances)) {
                if (SWAGGER_V3.equalsIgnoreCase(instances.get(0).getMetadata().get("swagger"))) {
                    swaggerVersion = swaggerVersion3;
                    apiDocsPath = apiDocsPath3;
                }
            }
            resources.add(swaggerResource(routeId, apiDocsPath, swaggerVersion));
        });

        return resources;
    }

    private SwaggerResource swaggerResource(String routeId, String apiDocsPath, String swaggerVersion) {
        String location = "/" + routeId + "/" + apiDocsPath;
        SwaggerResource swaggerResource = new SwaggerResource();
        swaggerResource.setName(routeId);
        swaggerResource.setUrl(location);
        swaggerResource.setLocation(location);
        swaggerResource.setSwaggerVersion(swaggerVersion);
        return swaggerResource;
    }
}
