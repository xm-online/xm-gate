package com.icthh.xm.gate.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Base64;
import java.util.Map;

import static com.icthh.xm.commons.security.XmAuthenticationConstants.AUTH_ADDITIONAL_DETAILS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TfaTokenDetectionFilterFunctionsUnitTest {

    private static final String TFA_VERIFICATION_KEY_CLAIM = "tfaVerificationKey";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ServerRequest serverRequest;

    @Mock
    private HandlerFunction<ServerResponse> next;

    @Mock
    private HttpServletRequest servletRequest;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private Jwt jwt;

    @Mock
    private ServerResponse mockResponse;

    private HandlerFilterFunction<ServerResponse, ServerResponse> filter;
    private MockedStatic<MvcUtils> mvcUtilsMock;

    @BeforeEach
    void setUp() {
        filter = TfaTokenDetectionFilterFunctions.tfaTokenDetection();
        when(serverRequest.servletRequest()).thenReturn(servletRequest);

        mvcUtilsMock = mockStatic(MvcUtils.class);
        mvcUtilsMock.when(() -> MvcUtils.getApplicationContext(serverRequest)).thenReturn(applicationContext);
    }

    @AfterEach
    void tearDown() {
        mvcUtilsMock.close();
    }

    @Test
    void tfaTokenDetection_shouldSkipFilter_forOAuthTokenEndpoint() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/uaa/oauth/token");
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void tfaTokenDetection_shouldSkipFilter_whenNoToken() throws Exception {
        when(servletRequest.getRequestURI()).thenReturn("/api/test");
        when(servletRequest.getHeader("Authorization")).thenReturn(null);
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void tfaTokenDetection_shouldSkipFilter_whenTokenNotTfa() throws Exception {
        String token = getTokenWithClaims(Map.of("sub", "user"));

        when(servletRequest.getRequestURI()).thenReturn("/api/test");
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void tfaTokenDetection_shouldProceed_whenTfaTokenWithoutVerificationKey() throws Exception {
        Jwt jwt = getJwtWithClaims(Map.of("sub", "user", "tfa", "true"));

        when(servletRequest.getRequestURI()).thenReturn("/api/test");
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + jwt.getTokenValue());

        when(applicationContext.getBean(JwtDecoder.class)).thenReturn(jwtDecoder);
        when(jwtDecoder.decode(eq(jwt.getTokenValue()))).thenReturn(jwt);
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void tfaTokenDetection_shouldReturnForbidden_whenTfaTokenUsedInappropriately() throws Exception {
        Jwt jwt = getJwtWithClaims(Map.of(
            "sub", "user",
            "tfa", "true",
            AUTH_ADDITIONAL_DETAILS, Map.of(TFA_VERIFICATION_KEY_CLAIM, "some-key")
        ));

        when(servletRequest.getRequestURI()).thenReturn("/api/test");
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + jwt.getTokenValue());

        when(applicationContext.getBean(JwtDecoder.class)).thenReturn(jwtDecoder);
        when(jwtDecoder.decode(eq(jwt.getTokenValue()))).thenReturn(jwt);

        ServerResponse response = filter.filter(serverRequest, next);

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode());
        verify(next, never()).handle(serverRequest);
    }

    @Test
    void tfaTokenDetection_shouldReturnInternalError_onException() throws Exception {
        Jwt jwt = getJwtWithClaims(Map.of("sub", "user", "tfa", "true"));

        when(servletRequest.getRequestURI()).thenReturn("/api/test");
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + jwt.getTokenValue());

        when(applicationContext.getBean(JwtDecoder.class)).thenReturn(jwtDecoder);
        when(jwtDecoder.decode(eq(jwt.getTokenValue()))).thenThrow(new RuntimeException("Decode error"));

        ServerResponse response = filter.filter(serverRequest, next);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode());
    }

    @Test
    void tfaTokenDetection_shouldProceed_whenTfaTokenWithEmptyDetails() throws Exception {
        Jwt jwt = getJwtWithClaims(Map.of(
            "sub", "user",
            "tfa", "true",
            AUTH_ADDITIONAL_DETAILS, Map.of()
        ));

        when(servletRequest.getRequestURI()).thenReturn("/api/test");
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + jwt.getTokenValue());

        when(jwtDecoder.decode(eq(jwt.getTokenValue()))).thenReturn(jwt);
        when(applicationContext.getBean(JwtDecoder.class)).thenReturn(jwtDecoder);
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @Test
    void tfaTokenDetection_shouldHandleClassCastException_gracefully() throws Exception {
        String token = getTokenWithClaims(Map.of("sub", "user", "tfa", "true"));

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim(AUTH_ADDITIONAL_DETAILS)).thenThrow(new ClassCastException("Cannot cast"));

        when(servletRequest.getRequestURI()).thenReturn("/api/test");
        when(servletRequest.getHeader("Authorization")).thenReturn("Bearer " + token);

        when(applicationContext.getBean(JwtDecoder.class)).thenReturn(jwtDecoder);
        when(jwtDecoder.decode(token)).thenReturn(jwt);
        when(next.handle(serverRequest)).thenReturn(mockResponse);

        ServerResponse response = filter.filter(serverRequest, next);

        verify(next).handle(serverRequest);
        assertEquals(mockResponse, response);
    }

    @SneakyThrows
    private String getTokenWithClaims(Map<String, Object> claims) {
        String header = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"none\"}".getBytes(UTF_8));

        String tfaPayload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(objectMapper.writeValueAsString(claims).getBytes(UTF_8));

        return header + "." + tfaPayload + ".signature";
    }

    private Jwt getJwtWithClaims(Map<String, Object> claims) {
        String token = getTokenWithClaims(claims);
        Jwt.Builder builder = Jwt.withTokenValue(token).header("alg", "none");
        claims.forEach(builder::claim);
        return builder.build();
    }
}
