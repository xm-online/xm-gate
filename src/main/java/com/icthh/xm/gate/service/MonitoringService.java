package com.icthh.xm.gate.service;

import com.icthh.xm.gate.web.rest.dto.ServiceMetrics;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Target;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonitoringService {

    public List<ServiceMetrics> getMetrics(String serviceName) {
        MetricsClient client = Feign.builder().target(Target.EmptyTarget.create(MetricsClient.class));

        String s = client.get(URI.create("http://localhost:8080"),
            "");
        log.info(s);
        return Collections.emptyList();
    }

    interface MetricsClient {

        @Headers("Authorization: Bearer {access_token}")
        @RequestLine("GET /management/metrics")
        String get(URI baseUrl, @Param("access_token") String accessToken);
    }
}
