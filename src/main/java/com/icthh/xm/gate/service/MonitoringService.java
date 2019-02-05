package com.icthh.xm.gate.service;

import static org.springframework.cloud.consul.discovery.ConsulServerUtils.findHost;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import com.icthh.xm.gate.web.client.MsServiceMetricsClient;
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
    private final MsServiceMetricsClient metricsClient;

    /**
     * Constructor of the service
     *
     * @param consulClient autowired consul client
     */
    public MonitoringService(ConsulClient consulClient) {
        this.consulClient = consulClient;
        this.metricsClient = Feign.builder().decoder(new JacksonDecoder())
            .target(Target.EmptyTarget.create(MsServiceMetricsClient.class));
    }

    /**
     * Gets all services.
     *
     * @return the list of services
     */
    public List<MsService> getServices() {
        Response<Map<String, List<String>>> catalogServices = consulClient.getCatalogServices(QueryParams.DEFAULT);
        List<MsService> dtoServices = new ArrayList<>();

        catalogServices.getValue().keySet().forEach(serviceId -> {
            Response<List<HealthService>> services = consulClient
                .getHealthServices(serviceId, false, null);
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


    /**
     * Get all metrics of each service instance
     * from endpoint /management/metrics
     *
     * @param serviceName name of the service
     * @return list of service metrics for each instance
     */
    public List<ServiceMetrics> getMetrics(String serviceName) {
        Map s = metricsClient.get(URI.create("http://localhost:8080"), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcmVhdGVUb2tlblRpbWUiOjE1NDkzNzY1Mjg4MjgsInVzZXJfbmFtZSI6ImNvbXBhc3MiLCJzY29wZSI6WyJvcGVuaWQiXSwicm9sZV9rZXkiOiJTVVBFUi1BRE1JTiIsInVzZXJfa2V5IjoiY29tcGFzcyIsImV4cCI6MTU0OTQxOTcyOCwiYWRkaXRpb25hbERldGFpbHMiOnt9LCJsb2dpbnMiOlt7InR5cGVLZXkiOiJMT0dJTi5FTUFJTCIsInN0YXRlS2V5IjpudWxsLCJsb2dpbiI6ImNvbXBhc3NAeG0tb25saW5lLmNvbS51YSJ9LHsidHlwZUtleSI6IkxPR0lOLk5JQ0tOQU1FIiwic3RhdGVLZXkiOm51bGwsImxvZ2luIjoiY29tcGFzcyJ9XSwiYXV0aG9yaXRpZXMiOlsiU1VQRVItQURNSU4iXSwianRpIjoiYTQ3MGY2YmItZDVkMC00ODljLTg0ODQtMTVmYTVlN2E1ZTEzIiwidGVuYW50IjoiQ09NUEFTUyIsImNsaWVudF9pZCI6ImludGVybmFsIn0.NHrWxI7NkmQYX6gLg04Eav8TT6el749tqt3EyE8kw74_k7S1EYD9gDRNx9lPxw3KJoHPcwUgFFJUrfnhgvYnA97mWFpMYuXZjMZKcN_d_66sz9Q7GDzSsjfdzT-orclEdrZj9zdl6HcuRwwyyo3dt3i1CF1rPg8mqbfLlm8g3IbLuHWGvlcBnrU_kw_tryAG3yNQ_KlJY78yTYR6Ssjs1wSPXA1fGB0d9FkY3GZ5kCaFEEEaoCtHGhS48B5oXSLP5rtU2bG7Boy24T9Ax_38wJDpGZ6aKXIZJow5nRcaPFMQ9Q4z7Nxv2gQoZOBcnrrHTMU9Yog8__VmMAlTTNhlbA");
        log.info(s.toString());
        return Collections.singletonList(ServiceMetrics.builder().metrics(s).instanceId("gate-4sfdf23").build());
    }

    interface HealthClient {

        @Headers("Authorization: {token}")
        @RequestLine("GET /management/health")
        String get(URI baseUrl, @Param("token") String accessToken);
    }

}
