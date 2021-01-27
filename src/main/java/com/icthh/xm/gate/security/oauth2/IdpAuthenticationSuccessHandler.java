package com.icthh.xm.gate.security.oauth2;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.gate.config.Constants.AUTH_RESPONSE_FIELD_BEARIRNG;
import static com.icthh.xm.gate.config.Constants.AUTH_RESPONSE_FIELD_IDP_TOKEN;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.idp.IdpConfigUtils;
import com.icthh.xm.commons.domain.idp.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.commons.domain.idp.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig.Features;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.gate.domain.idp.IdpConfigContainer;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
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

/**
 * XM Strategy used to handle a successful Auth0 user authentication.
 */
@Slf4j
@Component
public class IdpAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final TenantContextHolder tenantContextHolder;
    private final IdpConfigRepository idpConfigRepository;

    public IdpAuthenticationSuccessHandler(ObjectMapper objectMapper,
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
        Features features = getIdpClientConfig(tenantKey, authentication).getFeatures();

        if (features.isStateful()) {
            // TODO Stateful not implemented for now
            throw new UnsupportedOperationException("Stateful mode not supported yet");
        } else {
            ResponseEntity<Map<String, Object>> xmUaaTokenResponse = getXmUaaToken(tenantKey, authentication);
            prepareStatelessResponse(xmUaaTokenResponse, features, authentication, response);
        }
    }

    private IdpPublicClientConfig getIdpClientConfig(String tenantKey, Authentication authentication) {
        OAuth2AuthenticationToken authenticationToken = (OAuth2AuthenticationToken) authentication;
        String clientRegistrationId = authenticationToken.getAuthorizedClientRegistrationId();

        IdpConfigContainer idpConfigContainer = idpConfigRepository.getIdpClientConfigs()
            .getOrDefault(tenantKey, Collections.emptyMap())
            .get(clientRegistrationId);

        if (idpConfigContainer == null) {
            throw new BusinessException("IDP configuration not found for tenant: [" + tenantKey
                + "] and clientRegistrationId: [" + clientRegistrationId + "]");
        }

        return idpConfigContainer.getIdpPublicClientConfig();
    }

    private ResponseEntity<Map<String, Object>> getXmUaaToken(String tenantKey, Authentication authentication) {
        String idpIdToken = getIdpToken(authentication);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic aW50ZXJuYWw6aW50ZXJuYWw=");//TODO think how to provide creds here
        headers.set("x-tenant", tenantKey);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "idp_token");
        requestBody.add("token", idpIdToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        return restTemplate.exchange(
            "http://uaa/oauth/token",
            HttpMethod.POST,
            request,
            new ParameterizedTypeReference<>() {
            });
    }

    private String getIdpToken(Authentication authentication) {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        OidcIdToken oidcIdToken = oidcUser.getIdToken();
        return oidcIdToken.getTokenValue();
    }

    private void prepareStatelessResponse(ResponseEntity<Map<String, Object>> xmUaaTokenResponse,
                                          Features features,
                                          Authentication authentication,
                                          HttpServletResponse response) throws IOException {

        //set XM response status to authentication response
        response.setStatus(xmUaaTokenResponse.getStatusCodeValue());

        //copy XM headers to authentication response
        xmUaaTokenResponse.getHeaders().forEach((String headerName, List<String> headerValues)
            -> headerValues.forEach(headerValue -> response.addHeader(headerName, headerValue)));

        Map<String, Object> xmUaaTokenResponseBody = xmUaaTokenResponse.getBody();
        if (xmUaaTokenResponseBody == null) {
            throw new IllegalStateException("Uaa responded with empty body");
        }

        Map<String, Object> statelessResponse = new LinkedHashMap<>();

        //if bearirng feature is enabled - add IDP token to response
        if (features.getBearirng() != null && features.getBearirng().isEnabled()) {
            statelessResponse.put(AUTH_RESPONSE_FIELD_IDP_TOKEN, getIdpToken(authentication));
            statelessResponse.put(AUTH_RESPONSE_FIELD_BEARIRNG, features.getBearirng());
        }

        statelessResponse.putAll(xmUaaTokenResponseBody);
        response.getWriter().write(objectMapper.writeValueAsString(statelessResponse));
    }
}
