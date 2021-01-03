package com.icthh.xm.gate.security.oauth2;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.tenant.TenantContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class IdpSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final TenantContextHolder tenantContextHolder;
    private final IdpConfigRepository idpConfigRepository;


    public IdpSuccessHandler(ObjectMapper objectMapper,
                             @Lazy @Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate,
                             TenantContextHolder tenantContextHolder,
                             IdpConfigRepository idpConfigRepository) {
        this.objectMapper = objectMapper;
        this.tenantContextHolder = tenantContextHolder;
        this.restTemplate = restTemplate;
        this.idpConfigRepository = idpConfigRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String tenantKey = getRequiredTenantKeyValue(tenantContextHolder);
        String clientRegistrationId = getClientRegistrationId(authentication);
        String idpIdToken = getIdpToken(authentication);

        ResponseEntity<Map<String, ?>> xmUaaTokenResponse = exchangeIdpToXmToken(tenantKey, idpIdToken);
        //todo enrich with idp_token
        prepareResponse(xmUaaTokenResponse, response);
    }

    //TODO impl calling of POST /uaa/token?grant_type=idp_token&token={IDP_access_token}
    //TODO this is just stub for now
    private ResponseEntity<Map<String, ?>> exchangeIdpToXmToken(String tenantKey, String idpIdToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic aW50ZXJuYWw6aW50ZXJuYWw=");
        headers.set("x-tenant", tenantKey);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "password");
        requestBody.add("username", "xm");
        requestBody.add("password", "P@ssw0rd");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        return restTemplate.exchange(
            "http://uaa/oauth/token",
            HttpMethod.POST,
            request,
            new ParameterizedTypeReference<Map<String, ?>>() {
            });
    }

    private String getIdpToken(Authentication authentication) {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        OidcIdToken oidcIdToken = oidcUser.getIdToken();
        return oidcIdToken.getTokenValue();
    }

    private String getClientRegistrationId(Authentication authentication) {
        OAuth2AuthenticationToken authenticationToken = (OAuth2AuthenticationToken) authentication;
        return ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
    }

    private void prepareResponse(ResponseEntity<Map<String, ?>> xmUaaTokenResponse,
                                 HttpServletResponse response) throws IOException {

        xmUaaTokenResponse.getHeaders().forEach((String headerName, List<String> headerValues)
            -> headerValues.forEach(headerValue -> response.addHeader(headerName, headerValue)));

        response.setStatus(xmUaaTokenResponse.getStatusCodeValue());
        response.getWriter().write(objectMapper.writeValueAsString(xmUaaTokenResponse.getBody()));
    }
}
