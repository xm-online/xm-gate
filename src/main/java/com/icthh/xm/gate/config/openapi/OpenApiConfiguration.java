package com.icthh.xm.gate.config.openapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import tech.jhipster.config.JHipsterConstants;
import tech.jhipster.config.apidoc.customizer.JHipsterOpenApiCustomizer;

@Configuration
@Profile(JHipsterConstants.SPRING_PROFILE_API_DOCS)
public class OpenApiConfiguration {

    @Bean
    public DefaultServersOpenApiCustomizer defaultServersOpenApiCustomizer() {
        DefaultServersOpenApiCustomizer customizer = new DefaultServersOpenApiCustomizer();
        customizer.setOrder(JHipsterOpenApiCustomizer.DEFAULT_ORDER + 1);
        return customizer;
    }
}
