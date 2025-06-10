package com.icthh.xm.gate.web.rest;

import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import com.icthh.xm.gate.service.MonitoringService;
import com.icthh.xm.gate.web.rest.dto.MsService;
import com.icthh.xm.gate.web.rest.dto.ServiceHealth;
import com.icthh.xm.gate.web.rest.dto.ServiceMetrics;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for microservices monitoring.
 */
@RestController
@RequestMapping("/api/monitoring")
@ConditionalOnProperty(prefix = "application.monitoring.api", name = "enabled", havingValue = "true", matchIfMissing = false)
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
    @PrivilegeDescription("Privilege to get list of service instances")
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
    @PrivilegeDescription("Privilege to get heath of each service instance from endpoint /management/health")
    public ResponseEntity<List<ServiceHealth>> getHealth(@PathVariable String serviceName) {
        Objects.requireNonNull(serviceName, "Can't getMetrics health because serviceName is not pass");
        List<ServiceHealth> health = monitoringService.getHealth(serviceName);
        return ResponseEntity.ok(health);
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
    @PrivilegeDescription("Privilege to get all metrics of each service instance")
    public ResponseEntity<List<ServiceMetrics>> getMetrics(@PathVariable String serviceName) {
        Objects.requireNonNull(serviceName, "Can't getMetrics metrics because serviceName is not pass");
        List<ServiceMetrics> metrics = monitoringService.getMetrics(serviceName);
        return ResponseEntity.ok(metrics);
    }
}
