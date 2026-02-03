package com.icthh.xm.gate.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for RestClient beans.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientConfiguration {

    @LoadBalanced
    @Bean
    public RestClient loadBalancedRestClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
