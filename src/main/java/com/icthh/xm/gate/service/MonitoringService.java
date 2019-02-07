package com.icthh.xm.gate.service;

import static org.apache.http.HttpHost.DEFAULT_SCHEME_NAME;
import static org.springframework.cloud.consul.discovery.ConsulServerUtils.findHost;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.gate.web.client.MsServiceMetricsClient;
import com.icthh.xm.gate.web.rest.dto.MsService;
import com.icthh.xm.gate.web.rest.dto.ServiceInstance;
import com.icthh.xm.gate.web.rest.dto.ServiceMetrics;

import feign.Feign;
import feign.Target;
import feign.jackson.JacksonDecoder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class MonitoringService {

    private final ConsulClient consulClient;
    private final MsServiceMetricsClient metricsClient;
    private final XmAuthenticationContextHolder authContextHolder;

    /**
     * Constructor of the service
     *
     * @param consulClient autowired consul client
     */
    public MonitoringService(ConsulClient consulClient,
                             XmAuthenticationContextHolder authContextHolder,
                             MsServiceMetricsClient metricsClient) {
        this.consulClient = consulClient;
        this.authContextHolder = authContextHolder;
        this.metricsClient = metricsClient;
    }

    /**
     * Gets all services.
     *
     * @return the list of services
     */
    public List<MsService> getServices() {
        Response<Map<String, List<String>>> catalogServices = consulClient.getCatalogServices(QueryParams.DEFAULT);
        List<MsService> dtoServices = new ArrayList<>();

        if (catalogServices == null || MapUtils.isEmpty(catalogServices.getValue())) {
            return Collections.emptyList();
        }
        catalogServices.getValue().keySet().forEach(serviceId -> dtoServices.add(getService(serviceId)));
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
        List<ServiceMetrics> serviceMetrics = new LinkedList<>();
        MsService service = getService(serviceName);
        XmAuthenticationContext authContext = authContextHolder.getContext();
        String currentUserToken = authContext.getTokenValue().orElse("");

        service.getInstances().forEach(instance -> {
            URI serviceAdr = UriComponentsBuilder.newInstance()
                .scheme(DEFAULT_SCHEME_NAME)
                .host(instance.getAddress())
                .port(instance.getPort())
                .build()
                .toUri();
            Map metrics = metricsClient.get(serviceAdr, currentUserToken);

            serviceMetrics.add(ServiceMetrics.builder()
                .metrics(metrics)
                .instanceId(instance.getId())
                .build());
        });

        return serviceMetrics;
    }

    private MsService getService(String serviceName) {
        Response<List<HealthService>> services = consulClient
            .getHealthServices(serviceName, false, null);

        List<ServiceInstance> instances = new ArrayList<>();

        services.getValue().forEach(service -> {
            String host = findHost(service);
            instances.add(ServiceInstance.builder()
                .id(service.getService().getId())
                .address(host)
                .port(service.getService().getPort())
                .build());
        });

        return MsService.builder()
            .name(serviceName)
            .instances(instances)
            .build();
    }
}
