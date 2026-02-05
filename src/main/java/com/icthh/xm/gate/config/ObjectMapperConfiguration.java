package com.icthh.xm.gate.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ObjectMapperConfiguration {

    @Bean
    @Primary // for unqualified injection
    public ObjectMapper jsonObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ObjectMapper ymlObjectMapper() {
        return new ObjectMapper(new YAMLFactory());
    }
}
