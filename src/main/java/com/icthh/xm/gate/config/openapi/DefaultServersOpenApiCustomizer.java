package com.icthh.xm.gate.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.core.Ordered;

public class DefaultServersOpenApiCustomizer implements OpenApiCustomizer, Ordered {

    public static final String DEFAULT_SERVER_DESCRIPTION = "Generated server url";
    private int order = 1;

    @Override
    public void customise(OpenAPI openAPI) {
        if (openAPI.getServers() != null && openAPI.getServers().size() > 1) {

            openAPI.setServers(openAPI.getServers().stream()
                .filter(s -> !DEFAULT_SERVER_DESCRIPTION.equals(s.getDescription()))
                .toList());
        }
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }
}
