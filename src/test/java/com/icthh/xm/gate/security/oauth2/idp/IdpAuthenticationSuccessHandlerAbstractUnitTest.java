package com.icthh.xm.gate.security.oauth2.idp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tech.jhipster.config.JHipsterProperties;

import java.util.Map;
import java.util.function.Consumer;

import static com.icthh.xm.gate.config.Constants.HEADER_TENANT;
import static com.icthh.xm.gate.security.oauth2.idp.IdpTestUtils.buildAuthentication;
import static java.nio.charset.Charset.defaultCharset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdpAuthenticationSuccessHandlerAbstractUnitTest extends IdpAbstractUnitTest {

    private static final String UAA_TOKEN_URL = "http://uaa/oauth/token";
    private static final String GRANT_TYPE_ATTR = "grant_type";
    private static final String GRANT_TYPE_IDP_TOKEN = "idp_token";
    private static final String TOKEN_ATTR = "token";

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Captor
    private ArgumentCaptor<Consumer<HttpHeaders>> headersCaptor;
    @Captor
    private ArgumentCaptor<MultiValueMap<String, String>> bodyCaptor;

    private JHipsterProperties jhipsterProperties;
    private IdpAuthenticationSuccessHandler idpAuthenticationSuccessHandler;

    @BeforeEach
    void setUp() {
        jhipsterProperties = new JHipsterProperties();
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    void shouldThrowException_whenStatefulModeEnabled() {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 1;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true, true);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);
        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        jhipsterProperties.getSecurity().getClientAuthorization().setAccessTokenUri(UAA_TOKEN_URL);

        idpAuthenticationSuccessHandler = new IdpAuthenticationSuccessHandler(
            objectMapper, restClient, tenantContextHolder, idpConfigRepository, jhipsterProperties);

        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> idpAuthenticationSuccessHandler.onAuthenticationSuccess(null, null, null)
        );

        assertEquals("Stateful mode not supported yet", exception.getMessage());
    }

    @SneakyThrows
    @Test
    void shouldReturnTokenData_whenStatelessModeEnabled() {
        String tenantKey = "tenant1";
        String clientKeyPrefix = "Auth0_";
        int clientsAmount = 1;

        IdpPublicConfig idpPublicConfig = registerPublicConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true, false);
        IdpPrivateConfig idpPrivateConfig = registerPrivateConfigs(clientKeyPrefix, tenantKey, clientsAmount, true, true);
        validateRegistration(tenantKey, clientKeyPrefix, clientsAmount, idpPublicConfig, idpPrivateConfig);

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        jhipsterProperties.getSecurity().getClientAuthorization().setAccessTokenUri(UAA_TOKEN_URL);

        setupRestClientMock();

        idpAuthenticationSuccessHandler = new IdpAuthenticationSuccessHandler(
            objectMapper, restClient, tenantContextHolder, idpConfigRepository, jhipsterProperties);

        MockHttpServletResponse response = new MockHttpServletResponse();
        idpAuthenticationSuccessHandler.onAuthenticationSuccess(null, response, buildAuthentication("email"));

        verifyRestClientInteraction(tenantKey);
        JSONAssert.assertEquals(
            readConfig("idp/idp-auth-success_response.json"),
            response.getContentAsString(),
            JSONCompareMode.LENIENT
        );
    }

    private void setupRestClientMock() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq(UAA_TOKEN_URL))).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any(Consumer.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(MultiValueMap.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(any(ParameterizedTypeReference.class))).thenReturn(buildAuthTokenResponse());
    }

    private void verifyRestClientInteraction(String tenantKey) {
        verify(restClient).post();
        verify(requestBodyUriSpec).uri(eq(UAA_TOKEN_URL));
        verify(requestBodySpec).headers(headersCaptor.capture());
        verify(requestBodySpec).body(bodyCaptor.capture());
        verify(requestBodySpec).retrieve();

        validateCapturedHeaders(tenantKey);
        validateCapturedBody();
    }

    private void validateCapturedHeaders(String tenantKey) {
        HttpHeaders headers = new HttpHeaders();
        headersCaptor.getValue().accept(headers);

        assertEquals(MediaType.APPLICATION_FORM_URLENCODED, headers.getContentType());
        assertEquals(tenantKey, headers.getFirst(HEADER_TENANT));
        assertNotNull(headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    private void validateCapturedBody() {
        MultiValueMap<String, String> body = bodyCaptor.getValue();
        assertNotNull(body);
        assertEquals(GRANT_TYPE_IDP_TOKEN, body.getFirst(GRANT_TYPE_ATTR));
        assertEquals("idp.token.value", body.getFirst(TOKEN_ATTR));
    }

    @SneakyThrows
    private ResponseEntity<Map<String, Object>> buildAuthTokenResponse() {
        String stringBody = readConfig("idp/idp-auth-token-request.json");
        Map<String, Object> body = objectMapper.readValue(stringBody, new TypeReference<>() {});
        return ResponseEntity.ok().body(body);
    }

    @SneakyThrows
    private String readConfig(String name) {
        return IOUtils.toString(this.getClass().getResourceAsStream("/config/templates/" + name), defaultCharset());
    }
}
