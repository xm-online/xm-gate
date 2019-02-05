package com.icthh.xm.gate.web.client;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.net.URI;
import java.util.Map;

public interface MsServiceMetricsClient {

    @Headers("Authorization: Bearer {access_token}")
    @RequestLine("GET /management/metrics")
    Map get(URI baseUrl, @Param("access_token") String accessToken);
}
