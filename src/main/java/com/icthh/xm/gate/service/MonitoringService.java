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
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcmVhdGVUb2tlblRpbWUiOjE1NDkyOTA1MzA5NTcsInVzZXJfbmFtZSI6ImNvbXBhc3MiLCJzY29wZSI6WyJvcGVuaWQiXSwicm9sZV9rZXkiOiJTVVBFUi1BRE1JTiIsInVzZXJfa2V5IjoiY29tcGFzcyIsImV4cCI6MTU0OTMzMzczMCwiYWRkaXRpb25hbERldGFpbHMiOnt9LCJsb2dpbnMiOlt7InR5cGVLZXkiOiJMT0dJTi5FTUFJTCIsInN0YXRlS2V5IjpudWxsLCJsb2dpbiI6ImNvbXBhc3NAeG0tb25saW5lLmNvbS51YSJ9LHsidHlwZUtleSI6IkxPR0lOLk5JQ0tOQU1FIiwic3RhdGVLZXkiOm51bGwsImxvZ2luIjoiY29tcGFzcyJ9XSwiYXV0aG9yaXRpZXMiOlsiU1VQRVItQURNSU4iXSwianRpIjoiMjg1YmJlOTUtYjBhOS00YzRjLThiMjAtYTczNWU4YjVlYjg1IiwidGVuYW50IjoiQ09NUEFTUyIsImNsaWVudF9pZCI6ImludGVybmFsIn0.kFpu6R-yM9Z4aQqmSdr38eH0StY_7qeGdcLLrm3u7ZMlj9tGdhAg_6ZeuBjG3IOrUpEKZHHXAo9qFVb2MvMF0I_LiJPcjTj9g4rnN_N5ZL1BemxyFlETWsiyqDKy2EjLxJbvuqV2nmfDebnpZ7PxlIZgpTUxvlFD6p0nnG80plaabYDgz-Iyrd_gWNWuBzIEF_cuyyunZ21r0tC0fy_VPXA1J0MK-GDRpvxbd9i6tLznYBV3KPpR_TSMWmPESeYenSPP4UU2rhtJSPAPttA9HuejAfmkiv9jPnSG5Hnmt4oLXenwj6g6y4H8hle4uqir4yliWe9uBjm93xUIsxioOA");
        log.info(s);
        return Collections.emptyList();
    }

    interface MetricsClient {

        @Headers("Authorization: Bearer {access_token}")
        @RequestLine("GET /management/metrics")
        String get(URI baseUrl, @Param("access_token") String accessToken);
    }
}
