package com.icthh.xm.gate.web.rest;

import com.icthh.xm.gate.service.MonitoringService;
import com.icthh.xm.gate.web.rest.dto.MsService;
import com.icthh.xm.gate.web.rest.dto.ServiceHealth;
import com.icthh.xm.gate.web.rest.dto.ServiceMetrics;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * REST controller for microservices monitoring
 */
@RestController
@RequestMapping("/api/monitoring")
public class MonitoringResource {

    private final MonitoringService monitoringService;

    public MonitoringResource(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * GET /services : Get list of service instances
     *
     * @return the ResponseEntity with status 200 (OK) and the list of service instances
     */
    @GetMapping("/services")
    @PostAuthorize("hasPermission({'returnObject': returnObject}, 'GATE.MONITORING.SERVICE.GET_LIST')")
    public ResponseEntity<List<MsService>> getServices() {
        List<MsService> services = monitoringService.getServices();
        return ResponseEntity.ok(services);
    }

    /**
     * GET /services/{serviceName}/health : Get heath of each service instance from endpoint /management/health
     *
     * @param serviceName name of the service
     * @return the ResponseEntity with status 200 (OK) and the list of service health for each instance
     */
    @GetMapping("/services/{serviceName}/health")
    @PostAuthorize("hasPermission({'returnObject': returnObject}, 'GATE.MONITORING.SERVICE.GET_HEALTH')")
    public ResponseEntity<List<ServiceHealth>> getHealth(@PathVariable String serviceName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * GET /services/{serviceName}/metrics : Get all metrics of each service instance
     * from endpoint /management/metrics
     *
     * @param serviceName name of the service
     * @return the ResponseEntity with status 200 (OK) and the list of service metrics for each instance
     */
    @GetMapping("/services/{serviceName}/metrics")
    @PostAuthorize("hasPermission({'returnObject': returnObject}, 'GATE.MONITORING.SERVICE.GET_METRIC')")
    public ResponseEntity<List<ServiceMetrics>> getMetrics(@PathVariable String serviceName) {
        Objects.requireNonNull(serviceName, "Can't get metrics because serviceName is not pass");
        List<ServiceMetrics> metrics = monitoringService.getMetrics(serviceName);
        return ResponseEntity.ok(metrics);
    }
}
