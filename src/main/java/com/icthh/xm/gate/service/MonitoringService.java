package com.icthh.xm.gate.service;

import com.icthh.xm.gate.web.rest.dto.ServiceMetrics;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.Target;
import feign.jackson.JacksonDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MonitoringService {

    private final MetricsClient metricsClient = Feign.builder()
        .decoder(new JacksonDecoder())
        .target(Target.EmptyTarget.create(MetricsClient.class));

    public List<ServiceMetrics> getMetrics(String serviceName) {
        Map s = metricsClient.get(URI.create("http://localhost:8080"), "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcmVhdGVUb2tlblRpbWUiOjE1NDkyOTI4NjcwODEsInVzZXJfbmFtZSI6ImNvbXBhc3MiLCJzY29wZSI6WyJvcGVuaWQiXSwicm9sZV9rZXkiOiJTVVBFUi1BRE1JTiIsInVzZXJfa2V5IjoiY29tcGFzcyIsImV4cCI6MTU0OTMzNjA2NywiYWRkaXRpb25hbERldGFpbHMiOnt9LCJsb2dpbnMiOlt7InR5cGVLZXkiOiJMT0dJTi5FTUFJTCIsInN0YXRlS2V5IjpudWxsLCJsb2dpbiI6ImNvbXBhc3NAeG0tb25saW5lLmNvbS51YSJ9LHsidHlwZUtleSI6IkxPR0lOLk5JQ0tOQU1FIiwic3RhdGVLZXkiOm51bGwsImxvZ2luIjoiY29tcGFzcyJ9XSwiYXV0aG9yaXRpZXMiOlsiU1VQRVItQURNSU4iXSwianRpIjoiYTgxNTgwM2MtNzY5Yy00ZGRlLWE5MmEtNTY5ZTZjYjBhNGNjIiwidGVuYW50IjoiQ09NUEFTUyIsImNsaWVudF9pZCI6ImludGVybmFsIn0.X2yKzYW7gO8cWJzdcfnwcjY28IUuioKXuGI9MNMlUZ_MfXoY69dHbm4wXiutbSjIR1IPq2XHCzCHnRtzCAIA6wacYpfh9X-vmG4RfCOzEsSHD-R-72RpCso6ExByAGJM_LBKgseH8Fo-65915zwQowvTBawgKNbxzolEJskgiym9KmOX6SxvKSLdTh8kk9DgdGh-9wVeCywSe2uqoviFbX5A-xGhwn_5ddlnrxyt1UwmBE_8CvHAPka1AOoKBrpDk-zrg0EPE-YlL2J75O8z1cKLMZWF7S6TgB_pJ-62b8V3EWIyCgsLhrTAnijTX6myLWdWBz6JNWV0u3lqki0uyw");
        log.info(s.toString());
        return Collections.singletonList(ServiceMetrics.builder().metrics(s).instanceId("gate-4sfdf23").build());
    }

    interface MetricsClient {

        @Headers("Authorization: Bearer {access_token}")
        @RequestLine("GET /management/metrics")
        Map get(URI baseUrl, @Param("access_token") String accessToken);
    }
}
