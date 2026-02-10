package com.icthh.xm.gate.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.icthh.xm.gate.config.Constants.SPRING_PROFILE_API_DOCS;

@Configuration
@Profile(SPRING_PROFILE_API_DOCS)
@OpenAPIDefinition(info = @Info(title = "Gateway API", description = "Gateway API Documentation"))
public class OpenApiConfiguration {

}
