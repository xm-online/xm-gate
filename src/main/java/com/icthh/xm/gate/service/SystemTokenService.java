package com.icthh.xm.gate.service;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantKey;
import com.icthh.xm.gate.domain.TokenHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class SystemTokenService {

    private static final String GRANT_TYPE = "grant_type";
    private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    private static final String AUTHORIZATION = "Authorization";
    private static final String TOKEN_TYPE = "token_type";
    private static final String ACCESS_TOKEN = "access_token";


    private final RestTemplate restTemplate;
    private final TenantContextHolder tenantContext;
    private final TenantPropertiesService credService;

    public SystemTokenService(@Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate, TenantContextHolder tenantContext, TenantPropertiesService credService) {
        this.restTemplate = restTemplate;
        this.tenantContext = tenantContext;
        this.credService = credService;
    }

    protected Map post(String url,
                       Map<String, String> args,
                       Map<String, String> additionalHeaders,
                       MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> arg : args.entrySet()) {
            map.add(arg.getKey(), arg.getValue());
        }

        for (Map.Entry<String, String> addHeader : additionalHeaders.entrySet()) {
            headers.set(addHeader.getKey(), addHeader.getValue());
        }

        Optional<TenantKey> tenantKey = tenantContext.getContext().getTenantKey();
        tenantKey.ifPresent(tenantKey1 -> headers.set("x-tenant", tenantKey1.getValue()));

        HttpEntity<MultiValueMap> request = new HttpEntity<>(map, headers);
        log.info("Post to {} with args {}", url, args);
        return restTemplate.postForEntity(url, request, Map.class).getBody();
    }


    protected String getSystemToken() {

        Map<String, String> body = new HashMap<>();
        body.put(GRANT_TYPE, GRANT_TYPE_CLIENT_CREDENTIALS);
        TokenHolder uaa = credService.getTenantProps().getToken();

        Map<String, String> headers = new HashMap<>();
        headers.put(AUTHORIZATION, uaa.getSystemClientToken());
        Map response = this.post(
            uaa.getSystemAuthUrl(),
            body,
            headers,
            MediaType.APPLICATION_FORM_URLENCODED
        );

        String token = response.get(TOKEN_TYPE) + " " + response.get(ACCESS_TOKEN);
        log.info(token);
        return token;
    }

}
