package com.icthh.xm.gate.config;

import com.icthh.xm.gate.config.properties.ApplicationProperties;
import com.sun.net.httpserver.HttpServer;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RestClientConfigurationUnitTest {

    private static final String BROWSER_ACCEPT_ENCODING = "gzip, deflate, br";
    // not a real brotli stream: the proxy must not try to decode the body at all
    private static final byte[] BROTLI_BODY = {0x1b, 0x03, 0x00, (byte) 0xf8, 0x25, 0x00};

    private HttpServer server;
    private String baseUrl;
    private CloseableHttpClient httpClient;

    @BeforeEach
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add(HttpHeaders.LOCATION, baseUrl + "/target");
            exchange.sendResponseHeaders(HttpStatus.FOUND.value(), -1);
            exchange.close();
        });
        server.createContext("/redirect-br", exchange -> {
            exchange.getResponseHeaders().add(HttpHeaders.LOCATION, baseUrl + "/br");
            exchange.sendResponseHeaders(HttpStatus.FOUND.value(), -1);
            exchange.close();
        });
        server.createContext("/target", exchange -> send(exchange, "target".getBytes(UTF_8), null));
        server.createContext("/br", exchange -> send(exchange, BROTLI_BODY, "br"));
        server.createContext("/gzip", exchange -> send(exchange, gzip("plain"), "gzip"));
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
        server.stop(0);
    }

    @Test
    public void shouldReturnRedirectToClientByDefault() {
        ResponseEntity<byte[]> response = get(proxyRestClient(null, null), "/redirect", null);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(baseUrl + "/target", response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    public void shouldReturnRedirectToClientWhenFollowRedirectsDisabled() {
        ResponseEntity<byte[]> response = get(proxyRestClient(false, null), "/redirect", null);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(baseUrl + "/target", response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    public void shouldFollowRedirectWhenFollowRedirectsEnabled() {
        ResponseEntity<byte[]> response = get(proxyRestClient(true, null), "/redirect", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("target", new String(response.getBody(), UTF_8));
    }

    @Test
    public void shouldPassBrotliResponseThroughWithoutDecoding() {
        ResponseEntity<byte[]> response = get(proxyRestClient(null, null), "/br", BROWSER_ACCEPT_ENCODING);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("br", response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING));
        assertArrayEquals(BROTLI_BODY, response.getBody());
    }

    @Test
    public void shouldPassBrotliResponseThroughAfterFollowedRedirect() {
        ResponseEntity<byte[]> response = get(proxyRestClient(true, null), "/redirect-br", BROWSER_ACCEPT_ENCODING);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("br", response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING));
        assertArrayEquals(BROTLI_BODY, response.getBody());
    }

    @Test
    public void shouldPassGzipResponseThroughByDefault() throws IOException {
        ResponseEntity<byte[]> response = get(proxyRestClient(null, null), "/gzip", "gzip");

        assertEquals("gzip", response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING));
        assertArrayEquals(gzip("plain"), response.getBody());
    }

    @Test
    public void shouldDecompressGzipResponseWhenDecompressResponsesEnabled() {
        ResponseEntity<byte[]> response = get(proxyRestClient(null, true), "/gzip", "gzip");

        assertNull(response.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING));
        assertEquals("plain", new String(response.getBody(), UTF_8));
    }

    private RestClient proxyRestClient(Boolean followRedirects, Boolean decompressResponses) {
        ApplicationProperties.HttpClient httpClientProperties = new ApplicationProperties.HttpClient();
        httpClientProperties.setMaxConnections(10);
        httpClientProperties.setMaxConnectionsPerRoute(10);
        httpClientProperties.setConnectionTimeoutSeconds(5);
        httpClientProperties.setSocketTimeoutSeconds(5);
        httpClientProperties.setResponseTimeoutSeconds(5);
        httpClientProperties.setConnectionTtlSeconds(60);
        httpClientProperties.setEvictIdleSeconds(60);
        httpClientProperties.setValidateAfterInactivitySeconds(5);
        if (followRedirects != null) {
            httpClientProperties.setFollowRedirects(followRedirects);
        }
        if (decompressResponses != null) {
            httpClientProperties.setDecompressResponses(decompressResponses);
        }
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.setHttpClient(httpClientProperties);

        httpClient = new RestClientConfiguration(applicationProperties, new GatewayMvcProperties())
            .proxyHttpClient();
        return RestClient.builder()
            .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
            .build();
    }

    private ResponseEntity<byte[]> get(RestClient restClient, String path, String acceptEncoding) {
        return restClient.get()
            .uri(baseUrl + path)
            .headers(headers -> {
                if (acceptEncoding != null) {
                    headers.set(HttpHeaders.ACCEPT_ENCODING, acceptEncoding);
                }
            })
            .retrieve()
            .toEntity(byte[].class);
    }

    private static byte[] gzip(String value) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(value.getBytes(UTF_8));
        }
        return out.toByteArray();
    }

    private static void send(com.sun.net.httpserver.HttpExchange exchange, byte[] body, String contentEncoding)
        throws IOException {
        if (contentEncoding != null) {
            exchange.getResponseHeaders().add(HttpHeaders.CONTENT_ENCODING, contentEncoding);
        }
        exchange.sendResponseHeaders(HttpStatus.OK.value(), body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
