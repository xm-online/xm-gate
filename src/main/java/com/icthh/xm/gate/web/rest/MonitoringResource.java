package com.icthh.xm.gate.web.rest;

import com.icthh.xm.gate.web.rest.dto.Service;
import com.icthh.xm.gate.web.rest.dto.ServiceHealth;
import com.icthh.xm.gate.web.rest.dto.ServiceMetrics;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringResource {

    @GetMapping("/services")
    @PostAuthorize("hasPermission({'returnObject': returnObject}, 'GATE.MONITORING.SERVICE.GET_LIST')")
    public ResponseEntity<List<Service>> getServices() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @GetMapping("/services/{serviceName}/health")
    @PostAuthorize("hasPermission({'returnObject': returnObject}, 'GATE.MONITORING.SERVICE.GET_HEALTH')")
    public ResponseEntity<List<ServiceHealth>> getHealth(@PathVariable String serviceName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @GetMapping("/services/{serviceName}/metrics")
    @PostAuthorize("hasPermission({'returnObject': returnObject}, 'GATE.MONITORING.SERVICE.GET_METRIC')")
    public ResponseEntity<List<ServiceMetrics>> getMetrics(@PathVariable String serviceName) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
