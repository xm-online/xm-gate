package com.icthh.xm.gate.config;

import com.icthh.xm.gate.config.properties.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.handler.RestClientProxyExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuration for RestClient beans.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestClientConfiguration {

    private final ApplicationProperties applicationProperties;
    private final GatewayMvcProperties gatewayMvcProperties;

    @Bean
    @Primary // for unqualified injection
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient loadBalancedRestClient(@LoadBalanced RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RestClientProxyExchange restClientProxyExchange() {
        ApplicationProperties.HttpClient propertiesHttpClient = applicationProperties.getHttpClient();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(propertiesHttpClient.getMaxConnections())
            .setMaxConnPerRoute(propertiesHttpClient.getMaxConnectionsPerRoute())
            .setDefaultSocketConfig(SocketConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(propertiesHttpClient.getSocketTimeoutSeconds()))
                .build())
            .setDefaultConnectionConfig(ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(propertiesHttpClient.getConnectionTimeoutSeconds()))
                .setTimeToLive(TimeValue.ofSeconds(propertiesHttpClient.getConnectionTtlSeconds()))
                .setValidateAfterInactivity(TimeValue.ofSeconds(5))
                .build())
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(propertiesHttpClient.getResponseTimeoutSeconds()))
                .build())
            .evictIdleConnections(TimeValue.ofSeconds(propertiesHttpClient.getEvictIdleSeconds()))
            .evictExpiredConnections()
            .build();

        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory(httpClient);

        RestClient restClient = RestClient.builder()
            .requestFactory(factory)
            .build();

        return new RestClientProxyExchange(restClient, gatewayMvcProperties);
    }
}
