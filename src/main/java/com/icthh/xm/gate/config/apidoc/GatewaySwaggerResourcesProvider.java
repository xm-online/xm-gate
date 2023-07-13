package com.icthh.xm.gate.config.apidoc;

import java.util.ArrayList;
import java.util.List;

import io.github.jhipster.config.JHipsterConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

/**
 * Retrieves all registered microservices Swagger resources.
 */
@Component
@Primary
@Profile(JHipsterConstants.SPRING_PROFILE_SWAGGER)
@RequiredArgsConstructor
@Slf4j
public class GatewaySwaggerResourcesProvider implements SwaggerResourcesProvider {

    private final String defaultSwaggerVersion = "2.0";
    private final String defaultApiDocsPath = "v2/api-docs";
    private final String SWAGGER_V3 = "v3";

    private final RouteLocator routeLocator;

    private final DiscoveryClient discoveryClient;


    @Override
    public List<SwaggerResource> get() {
        List<SwaggerResource> resources = new ArrayList<>();

        //Add the default swagger resource that correspond to the gateway's own swagger doc
        resources.add(swaggerResource("default", defaultApiDocsPath, defaultSwaggerVersion));

        //Add the registered microservices swagger docs as additional swagger resources
        List<Route> routes = routeLocator.getRoutes();
        routes.forEach(route -> {
            List<ServiceInstance> instances = discoveryClient.getInstances(route.getId());
            log.debug("route {} instances.size={}", route.getId(), instances.size());
            String swaggerVersion = defaultSwaggerVersion;
            String apiDocsPath = defaultApiDocsPath;
            if (CollectionUtils.isNotEmpty(instances)) {
                if (SWAGGER_V3.equalsIgnoreCase(instances.get(0).getMetadata().get("swagger"))) {
                    swaggerVersion = "3.0";
                    apiDocsPath = "v3/api-docs";
                }
            }
            resources.add(swaggerResource(route.getId(), route.getFullPath().replace("**", apiDocsPath), swaggerVersion));
        });

        return resources;
    }

    private SwaggerResource swaggerResource(String name, String location, String swaggerVersion) {
        SwaggerResource swaggerResource = new SwaggerResource();
        swaggerResource.setName(name);
        swaggerResource.setLocation(location);
        swaggerResource.setSwaggerVersion(swaggerVersion);
        return swaggerResource;
    }
}
