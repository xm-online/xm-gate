package com.icthh.xm.gate.security.oauth2;

import static com.icthh.xm.commons.tenant.TenantContextUtils.getRequiredTenantKeyValue;
import static com.icthh.xm.gate.config.Constants.AUTH_RESPONSE_FIELD_IDP_ACCESS_TOKEN_INCLUSION;
import static com.icthh.xm.gate.config.Constants.AUTH_RESPONSE_FIELD_IDP_TOKEN;
import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.Features;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import io.github.jhipster.config.JHipsterProperties;

import java.io.IOException;
import java.util.Base64;
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
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * XM Strategy used to handle a successful IDP user authentication.
 */
@Slf4j
@Component
public class IdpAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    public static final String GRANT_TYPE_ATTR = "grant_type";
    public static final String GRANT_TYPE_IDP_TOKEN = "idp_token";
    public static final String TOKEN_ATTR = "token";
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
        Features features = idpConfigRepository.getTenantFeatures(tenantKey);

        if (features.isStateful()) {
            // TODO Stateful not implemented for now
            throw new UnsupportedOperationException("Stateful mode not supported yet");
        } else {
            ResponseEntity<Map<String, Object>> xmUaaTokenResponse = getXmUaaToken(tenantKey, authentication);
            prepareStatelessResponse(xmUaaTokenResponse, features, authentication, response);
        }
    }

    private ResponseEntity<Map<String, Object>> getXmUaaToken(String tenantKey,
                                                              Authentication authentication) {
        HttpEntity<MultiValueMap<String, String>> uaaTokenRequest = buildUaaTokenRequest(tenantKey, authentication);

        return restTemplate.exchange(
            jhipsterProperties.getSecurity().getClientAuthorization().getAccessTokenUri(),
            HttpMethod.POST,
            uaaTokenRequest,
            // do not optimize code cause of compiler issue. It will broke pipeline
            // https://bugs.openjdk.java.net/browse/JDK-8212586
            new ParameterizedTypeReference<Map<String, Object>>() {
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
        xmUaaTokenResponse.getHeaders()
                          .forEach((header, values) -> addHeaderToResponse(response, header, values));

        Map<String, Object> xmUaaTokenResponseBody = xmUaaTokenResponse.getBody();
        if (xmUaaTokenResponseBody == null) {
            throw new IllegalStateException("Uaa responded with empty body");
        }

        Map<String, Object> statelessResponse = new LinkedHashMap<>();

        //if bearing feature is enabled - add IDP token to response
        if (features.getIdpAccessTokenInclusion() != null && features.getIdpAccessTokenInclusion().isEnabled()) {
            // FIXME: I'm not sure that we need to pass here ID token.
            //  Most probably it should be Access token because the idea of the feature that some system
            //  (like AWS Gateway) will authorize subsequent requests.
            statelessResponse.put(AUTH_RESPONSE_FIELD_IDP_TOKEN, getIdpToken(authentication));
            statelessResponse.put(AUTH_RESPONSE_FIELD_IDP_ACCESS_TOKEN_INCLUSION, features.getIdpAccessTokenInclusion());
        }

        statelessResponse.putAll(xmUaaTokenResponseBody);
        response.getWriter().write(objectMapper.writeValueAsString(statelessResponse));
    }

    private void addHeaderToResponse(HttpServletResponse response, String header, List<String> values) {
        values.forEach(value -> response.addHeader(header, value));
    }
}
