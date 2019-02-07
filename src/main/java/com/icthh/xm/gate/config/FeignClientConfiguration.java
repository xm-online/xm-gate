package com.icthh.xm.gate.config;

import com.icthh.xm.gate.web.client.MsMonitoringClient;
import feign.Feign;
import feign.Target;
import feign.jackson.JacksonDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfiguration {

    @Bean
    public MsMonitoringClient msServiceMetricsClient() {
        return Feign.builder().decoder(new JacksonDecoder())
            .target(Target.EmptyTarget.create(MsMonitoringClient.class));
    }
}
