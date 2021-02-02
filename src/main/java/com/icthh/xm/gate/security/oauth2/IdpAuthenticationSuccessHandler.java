package com.icthh.xm.gate.security.oauth2;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.gate.config.Constants.AUTH_RESPONSE_FIELD_BEARIRNG;
import static com.icthh.xm.gate.config.Constants.AUTH_RESPONSE_FIELD_IDP_TOKEN;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icthh.xm.commons.domain.idp.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.commons.domain.idp.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig.Features;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.gate.domain.idp.IdpConfigContainer;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.github.jhipster.config.JHipsterProperties;
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

    private static final String GRANT_TYPE_ATTR = "grant_type";
    private static final String GRANT_TYPE_IDP_TOKEN = "idp_token";
    private static final String TOKEN_ATTR = "token";
    public static final String COLON_SEPARATOR = ":";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final TenantContextHolder tenantContextHolder;
    private final IdpConfigRepository idpConfigRepository;
    private final JHipsterProperties jhipsterProperties;

    public IdpAuthenticationSuccessHandler(ObjectMapper objectMapper,
                                           @Lazy @Qualifier("loadBalancedRestTemplate") RestTemplate restTemplate,
                                           TenantContextHolder tenantContextHolder,
                                           IdpConfigRepository idpConfigRepository,
                                           JHipsterProperties jhipsterProperties) {
        this.objectMapper = objectMapper;
        this.tenantContextHolder = tenantContextHolder;
        this.restTemplate = restTemplate;
        this.idpConfigRepository = idpConfigRepository;
        this.jhipsterProperties = jhipsterProperties;
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

    private ResponseEntity<Map<String, Object>> getXmUaaToken(String tenantKey,
                                                              Authentication authentication) {
        HttpEntity<MultiValueMap<String, String>> uaaTokenRequest = buildUaaTokenRequest(tenantKey, authentication);

        return restTemplate.exchange(
            jhipsterProperties.getSecurity().getClientAuthorization().getAccessTokenUri(),
            HttpMethod.POST,
            uaaTokenRequest,
            new ParameterizedTypeReference<>() {
            });
    }

    private HttpEntity<MultiValueMap<String, String>> buildUaaTokenRequest(String tenantKey,
                                                                           Authentication authentication) {
        return new HttpEntity<>(buildRequestBody(getIdpToken(authentication)), buildHttpHeaders(tenantKey));
    }

    private MultiValueMap<String, String> buildRequestBody(String idpIdToken) {
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();

        requestBody.add(GRANT_TYPE_ATTR, GRANT_TYPE_IDP_TOKEN);
        requestBody.add(TOKEN_ATTR, idpIdToken);

        return requestBody;
    }

    private HttpHeaders buildHttpHeaders(String tenantKey) {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader());
        headers.set(HEADER_TENANT, tenantKey);

        return headers;
    }

    private String getIdpToken(Authentication authentication) {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        OidcIdToken oidcIdToken = oidcUser.getIdToken();
        return oidcIdToken.getTokenValue();
    }

    private String buildAuthorizationHeader() {
        JHipsterProperties.Security security = jhipsterProperties.getSecurity();

        String clientId = security.getClientAuthorization().getClientId();
        String clientSecret = security.getClientAuthorization().getClientSecret();
        byte[] bytes = (clientId + COLON_SEPARATOR + clientSecret).getBytes();

        return "Basic " + new String(Base64.getEncoder().encode(bytes));
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
