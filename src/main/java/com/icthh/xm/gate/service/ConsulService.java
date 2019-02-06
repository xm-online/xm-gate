package com.icthh.xm.gate.service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import com.icthh.xm.gate.config.ApplicationProperties;
import feign.Feign;
import feign.Target;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsulService {

    private final SystemTokenService systemTokenService;
    private final ApplicationProperties appProps;


    public void getHealth(String service) {
        ConsulClient client = new ConsulClient(appProps.getConsulAddress());
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
            .setPassing(true)
            .setQueryParams(QueryParams.DEFAULT)
            .build();

        String token = this.systemTokenService.getSystemToken();

        Response<List<HealthService>> healthyServices = client.getHealthServices(service, request);
        healthyServices.getValue().stream().forEach(healthService -> {
            MonitoringService.HealthClient mc = Feign.builder().target(Target.EmptyTarget.create(MonitoringService.HealthClient.class));
            // TODO remove http

            URI baseUrl = URI.create("http://" + healthService.getService().getAddress() + ":" + healthService.getService().getPort() + "/management/health");
            String s = mc.get(baseUrl, token);
            System.out.println(s);
        });
        System.out.println(healthyServices);

    }
}
