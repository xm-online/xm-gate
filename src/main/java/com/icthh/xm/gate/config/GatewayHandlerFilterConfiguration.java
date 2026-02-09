package com.icthh.xm.gate.config;

import com.icthh.xm.gate.gateway.functions.AccessControlFilterFunctions;
import com.icthh.xm.gate.gateway.functions.AddDomainRelayHeadersFunctions;
import com.icthh.xm.gate.gateway.functions.HighLogFilterFunctions;
import com.icthh.xm.gate.gateway.functions.IdpStatefulModeFilterFunctions;
import com.icthh.xm.gate.gateway.functions.LoggingFilterFunctions;
import com.icthh.xm.gate.gateway.functions.TfaTokenDetectionFilterFunctions;
import com.icthh.xm.gate.gateway.ratelimitting.RateLimitingFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayHandlerFilterConfiguration {

    @Bean
    public AddDomainRelayHeadersFunctions.FilterSupplier domainRelayFunctionsSupplier() {
        return new AddDomainRelayHeadersFunctions.FilterSupplier();
    }

    @Bean
    public HighLogFilterFunctions.FilterSupplier highlogFilterSupplier() {
        return new HighLogFilterFunctions.FilterSupplier();
    }

    @Bean
    public LoggingFilterFunctions.FilterSupplier loggingFilterSupplier() {
        return new LoggingFilterFunctions.FilterSupplier();
    }

    @Bean
    public TfaTokenDetectionFilterFunctions.FilterSupplier tfaTokenDetectionFilterSupplier() {
        return new TfaTokenDetectionFilterFunctions.FilterSupplier();
    }

    @Bean
    public IdpStatefulModeFilterFunctions.FilterSupplier idpStatefulModeFilterSupplier() {
        return new IdpStatefulModeFilterFunctions.FilterSupplier();
    }

    @Bean
    public AccessControlFilterFunctions.FilterSupplier accessControlFilterSupplier() {
        return new AccessControlFilterFunctions.FilterSupplier();
    }

    @Bean
    public RateLimitingFunctions.FilterSupplier rateLimittingFilterSupplier() {
        return new RateLimitingFunctions.FilterSupplier();
    }
}
