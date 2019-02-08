package com.icthh.xm.gate.config;

import com.icthh.xm.gate.web.client.MsMonitoringClient;
import feign.Feign;
import feign.Request;
import feign.Target;
import feign.jackson.JacksonDecoder;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfiguration {

    private FeignHttpClientProperties properties;

    public FeignClientConfiguration(FeignHttpClientProperties feignHttpClientProperties) {
        properties = feignHttpClientProperties;
    }

    @Bean
    public MsMonitoringClient msServiceMetricsClient() {
        return Feign.builder().decoder(new JacksonDecoder())
            .options(new Request.Options(properties.getConnectionTimeout(), properties.getConnectionTimeout()))
            .target(Target.EmptyTarget.create(MsMonitoringClient.class));
    }
}
