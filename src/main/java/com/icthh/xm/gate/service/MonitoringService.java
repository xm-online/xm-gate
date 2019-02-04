package com.icthh.xm.gate.service;

import com.icthh.xm.gate.web.rest.dto.ServiceMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonitoringService {

    public List<ServiceMetrics> getMetrics(String serviceName) {
        return Collections.emptyList();
    }
}
