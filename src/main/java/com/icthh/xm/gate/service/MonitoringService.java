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
    private final MetricsClient metricsClient = Feign.builder()
        .decoder(new JacksonDecoder())
        .target(Target.EmptyTarget.create(MetricsClient.class));


    public MonitoringService(ConsulClient consulClient) {
        this.consulClient = consulClient;
    }

    public List<ServiceMetrics> getMetrics(String serviceName) {
        MetricsClient client = Feign.builder()
            .decoder(new JacksonDecoder())
            .target(Target.EmptyTarget.create(MetricsClient.class));

    public List<ServiceMetrics> getMetrics(String serviceName) {
        Map s = metricsClient.get(URI.create("http://localhost:8080"), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcmVhdGVUb2tlblRpbWUiOjE1NDkyOTI4NjcwODEsInVzZXJfbmFtZSI6ImNvbXBhc3MiLCJzY29wZSI6WyJvcGVuaWQiXSwicm9sZV9rZXkiOiJTVVBFUi1BRE1JTiIsInVzZXJfa2V5IjoiY29tcGFzcyIsImV4cCI6MTU0OTMzNjA2NywiYWRkaXRpb25hbERldGFpbHMiOnt9LCJsb2dpbnMiOlt7InR5cGVLZXkiOiJMT0dJTi5FTUFJTCIsInN0YXRlS2V5IjpudWxsLCJsb2dpbiI6ImNvbXBhc3NAeG0tb25saW5lLmNvbS51YSJ9LHsidHlwZUtleSI6IkxPR0lOLk5JQ0tOQU1FIiwic3RhdGVLZXkiOm51bGwsImxvZ2luIjoiY29tcGFzcyJ9XSwiYXV0aG9yaXRpZXMiOlsiU1VQRVItQURNSU4iXSwianRpIjoiYTgxNTgwM2MtNzY5Yy00ZGRlLWE5MmEtNTY5ZTZjYjBhNGNjIiwidGVuYW50IjoiQ09NUEFTUyIsImNsaWVudF9pZCI6ImludGVybmFsIn0.X2yKzYW7gO8cWJzdcfnwcjY28IUuioKXuGI9MNMlUZ_MfXoY69dHbm4wXiutbSjIR1IPq2XHCzCHnRtzCAIA6wacYpfh9X-vmG4RfCOzEsSHD-R-72RpCso6ExByAGJM_LBKgseH8Fo-65915zwQowvTBawgKNbxzolEJskgiym9KmOX6SxvKSLdTh8kk9DgdGh-9wVeCywSe2uqoviFbX5A-xGhwn_5ddlnrxyt1UwmBE_8CvHAPka1AOoKBrpDk-zrg0EPE-YlL2J75O8z1cKLMZWF7S6TgB_pJ-62b8V3EWIyCgsLhrTAnijTX6myLWdWBz6JNWV0u3lqki0uyw");
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
