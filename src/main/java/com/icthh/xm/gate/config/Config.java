package com.icthh.xm.gate.config;

import io.micrometer.tracing.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    public Tracer tracer() {
        return Tracer.NOOP;
    }
}
