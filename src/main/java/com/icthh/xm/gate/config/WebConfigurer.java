package com.icthh.xm.gate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.gate.web.rest.errors.ExceptionTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.WebExceptionHandler;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.web.rest.errors.ReactiveWebExceptionHandler;

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
@Configuration
public class WebConfigurer implements WebFluxConfigurer {

    private final Logger log = LoggerFactory.getLogger(WebConfigurer.class);

    private final JHipsterProperties jHipsterProperties;

    public WebConfigurer(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = jHipsterProperties.getCors();
        if (!CollectionUtils.isEmpty(config.getAllowedOrigins()) || !CollectionUtils.isEmpty(config.getAllowedOriginPatterns())) {
            log.debug("Registering CORS filter");
            source.registerCorsConfiguration("/api/**", config);
            source.registerCorsConfiguration("/management/**", config);
            source.registerCorsConfiguration("/v3/api-docs", config);
            source.registerCorsConfiguration("/auth/**", config);
            source.registerCorsConfiguration("/*/api/**", config);
            source.registerCorsConfiguration("/*/management/**", config);
            source.registerCorsConfiguration("/*/oauth/**", config);
        }
        return source;
    }

    @Bean
    @Order(-2) // The handler must have precedence over WebFluxResponseStatusExceptionHandler and Spring Boot's ErrorWebExceptionHandler
    public WebExceptionHandler problemExceptionHandler(ObjectMapper mapper, ExceptionTranslator problemHandling) {
        return new ReactiveWebExceptionHandler(problemHandling, mapper);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }
}
