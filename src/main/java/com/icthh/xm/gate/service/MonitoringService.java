package com.icthh.xm.gate.service;

import static org.apache.http.HttpHost.DEFAULT_SCHEME_NAME;
import static org.springframework.cloud.consul.discovery.ConsulServerUtils.findHost;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.model.HealthService;
import com.icthh.xm.commons.security.XmAuthenticationContext;
import com.icthh.xm.commons.security.XmAuthenticationContextHolder;
import com.icthh.xm.gate.web.client.MsMonitoringClient;
import com.icthh.xm.gate.web.rest.dto.HealthStatus;
import com.icthh.xm.gate.web.rest.dto.HealthStatus.HealthStatusBuilder;
import com.icthh.xm.gate.web.rest.dto.MsService;
import com.icthh.xm.gate.web.rest.dto.ServiceHealth;
import com.icthh.xm.gate.web.rest.dto.ServiceHealth.ServiceHealthBuilder;
import com.icthh.xm.gate.web.rest.dto.ServiceInstance;
import com.icthh.xm.gate.web.rest.dto.ServiceMetrics;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class MonitoringService {

    private static final String ACTUATOR_HEALTH_STATUS = "status";
    private static final String ACTUATOR_HEALTH_DETAILS = "details";

    private final ConsulClient consulClient;
    private final MsMonitoringClient monitoringClient;
    private final XmAuthenticationContextHolder authContextHolder;

    /**
     * Constructor of the service
     *
     * @param consulClient autowired consul client
     */
    public MonitoringService(ConsulClient consulClient,
                             XmAuthenticationContextHolder authContextHolder,
                             MsMonitoringClient metricsClient) {
        this.consulClient = consulClient;
        this.authContextHolder = authContextHolder;
        this.monitoringClient = metricsClient;
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
     * Get health of each service instance
     * from endpoint /management/health
     *
     * @param serviceName name of the service
     * @return list of service health for each instance
     */
    @SuppressWarnings("unchecked")
    public List<ServiceHealth> getHealth(String serviceName) {
        List<ServiceHealth> serviceHealths = new LinkedList<>();
        MsService service = getService(serviceName);
        XmAuthenticationContext authContext = authContextHolder.getContext();
        String currentUserToken = authContext.getTokenValue().orElse("");

        service.getInstances().forEach(instance -> {
            URI serviceAdr = buildUri(instance);
            Map<String, Object> healthStatus = monitoringClient.getHealth(serviceAdr, currentUserToken);

            ServiceHealthBuilder serviceHealthBuilder = ServiceHealth.builder().instanceId(instance.getId());
            HealthStatusBuilder healthStatusBuilder = HealthStatus.builder();
            healthStatusBuilder.status(healthStatus.get(ACTUATOR_HEALTH_STATUS).toString());

            Object details = healthStatus.get(ACTUATOR_HEALTH_DETAILS);
            if (details == null) {
                //support spring actuator with version < 2.0
                healthStatusBuilder.details(
                    healthStatus.entrySet().stream()
                        .filter(x -> x.getKey().equals(ACTUATOR_HEALTH_STATUS))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                );
            } else {
                //support new version of spring actuator
                healthStatusBuilder.details((Map) details);
            }

            serviceHealthBuilder.health(healthStatusBuilder.build());
            serviceHealths.add(serviceHealthBuilder.build());
        });

        return serviceHealths;
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
            URI serviceAdr = buildUri(instance);
            Map metrics = monitoringClient.getMetrics(serviceAdr, currentUserToken);

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

    private URI buildUri(ServiceInstance instance) {
        return UriComponentsBuilder.newInstance()
            .scheme(DEFAULT_SCHEME_NAME)
            .host(instance.getAddress())
            .port(instance.getPort())
            .build()
            .toUri();
    }
}
