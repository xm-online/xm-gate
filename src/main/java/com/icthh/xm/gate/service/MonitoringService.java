package com.icthh.xm.gate.service;

import static org.springframework.cloud.consul.discovery.ConsulServerUtils.findHost;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import com.icthh.xm.gate.web.rest.dto.MsService;
import com.icthh.xm.gate.web.rest.dto.ServiceInstance;
import com.icthh.xm.gate.web.rest.dto.ServiceMetrics;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Target;
import feign.jackson.JacksonDecoder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MonitoringService {

    private final ConsulClient consulClient;

    public MonitoringService(ConsulClient consulClient) {
        this.consulClient = consulClient;
    }

    public List<ServiceMetrics> getMetrics(String serviceName) {
        MetricsClient client = Feign.builder()
            .decoder(new JacksonDecoder())
            .target(Target.EmptyTarget.create(MetricsClient.class));

        Map s = client.get(URI.create("http://localhost:8080"),
            "");
        log.info(s.toString());
        return Collections.singletonList(ServiceMetrics.builder().metrics(s).instanceId("gate-4sfdf23").build());
    }

    /**
     * Gets all services.
     * @return the list of services
     */
    public List<MsService> getServices() {

        Response<Map<String, List<String>>> catalogServices = consulClient.getCatalogServices(QueryParams.DEFAULT);
        List<MsService> dtoServices = new ArrayList<>();

        catalogServices.getValue().keySet().forEach(serviceId -> {
            Response<List<HealthService>> services = consulClient
                .getHealthServices(serviceId, true, null);
            List<ServiceInstance> instances = new ArrayList<>();

            services.getValue().forEach(service -> {
                String host = findHost(service);
                instances.add(ServiceInstance.builder()
                    .id(service.getService().getId())
                    .address(host)
                    .port(service.getService().getPort())
                    .build());
            });
            dtoServices.add(MsService.builder()
                .name(serviceId)
                .instances(instances)
                .build());
        });

        return dtoServices;
    }

    interface MetricsClient {

        @Headers("Authorization: Bearer {access_token}")
        @RequestLine("GET /management/metrics")
        Map get(URI baseUrl, @Param("access_token") String accessToken);
    }
}
