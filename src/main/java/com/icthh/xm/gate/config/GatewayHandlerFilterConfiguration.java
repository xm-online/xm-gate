package com.icthh.xm.gate.config;

import com.icthh.xm.gate.gateway.filter.AccessControlFilterFunctions;
import com.icthh.xm.gate.gateway.filter.AddDomainRelayHeadersFunctions;
import com.icthh.xm.gate.gateway.filter.HighLogFilterFunctions;
import com.icthh.xm.gate.gateway.filter.IdpStatefulModeFilterFunctions;
import com.icthh.xm.gate.gateway.filter.LoggingFilterFunctions;
import com.icthh.xm.gate.gateway.filter.TenantInitFilterFunctions;
import com.icthh.xm.gate.gateway.filter.TfaTokenDetectionFilterFunctions;
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
    public TenantInitFilterFunctions.FilterSupplier tenantInitFilterSupplier() {
        return new TenantInitFilterFunctions.FilterSupplier();
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
}
