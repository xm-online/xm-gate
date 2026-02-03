package com.icthh.xm.gate.config;

import com.icthh.xm.commons.metric.MetricsConfiguration;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.java21.instrument.binder.jdk.VirtualThreadMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for defining {@link MeterBinder} beans.
 * MeterBinders are bound to the meter registry in {@link MetricsConfiguration}.
 */
@Configuration
public class GatewayMetricsConfiguration extends MetricsConfiguration {

    @Bean
    public MeterBinder virtualThreadMetricsBinder() {
        return new VirtualThreadMetrics();
    }
}
