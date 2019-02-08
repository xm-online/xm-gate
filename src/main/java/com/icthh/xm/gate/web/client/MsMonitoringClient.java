package com.icthh.xm.gate.web.client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.net.URI;
import java.util.Map;

public interface MsMonitoringClient {

    @Headers("Authorization: Bearer {access_token}")
    @RequestLine("GET /management/metrics")
    Map<String, Object> getMetrics(URI baseUrl, @Param("access_token") String accessToken);

    @Headers("Authorization: Bearer {access_token}")
    @RequestLine("GET /management/health")
    Map<String, Object> getHealth(URI baseUrl, @Param("access_token") String accessToken);
}
