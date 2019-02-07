package com.icthh.xm.gate.web.client;

import com.icthh.xm.gate.domain.health.HealthResponse;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.net.URI;
import java.util.Map;

public interface MsServiceMonitoringClient {

    @Headers("Authorization: Bearer {access_token}")
    @RequestLine("GET /management/metrics")
    Map getMetrics(URI baseUrl, @Param("access_token") String accessToken);


    @Headers("Authorization: Bearer {access_token}")
    @RequestLine("GET /management/health")
    HealthResponse getHealth(URI baseUrl, @Param("access_token") String accessToken);

}
