package com.icthh.xm.gate.config;

import com.icthh.xm.gate.gateway.functions.AddDomainRelayHeadersFunctions;
import com.icthh.xm.gate.gateway.functions.IdpStatefulModeFilterFunctions;
import com.icthh.xm.gate.gateway.functions.TfaTokenDetectionFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayHandlerFilterConfiguration {

    @Bean
    public AddDomainRelayHeadersFunctions.FilterSupplier domainRelayFunctionsSupplier() {
        return new AddDomainRelayHeadersFunctions.FilterSupplier();
    }

    @Bean
    public TfaTokenDetectionFilterFunctions.FilterSupplier tfaTokenDetectionFilterSupplier() {
        return new TfaTokenDetectionFilterFunctions.FilterSupplier();
    }

    @Bean
    public IdpStatefulModeFilterFunctions.FilterSupplier idpStatefulModeFilterSupplier() {
        return new IdpStatefulModeFilterFunctions.FilterSupplier();
    }
}
