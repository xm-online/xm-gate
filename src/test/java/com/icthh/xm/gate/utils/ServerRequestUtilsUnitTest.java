package com.icthh.xm.gate.utils;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static com.icthh.xm.gate.utils.ServerRequestUtils.extractServiceName;
import static com.icthh.xm.gate.utils.ServerRequestUtils.getClientIdFromToken;
import static com.icthh.xm.gate.utils.ServerRequestUtils.normalizeApiPrefix;
import static com.icthh.xm.gate.utils.ServerRequestUtils.stripApiPrefix;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

class ServerRequestUtilsUnitTest {

    private static final String API_PREFIX = "/xm-api";
    private static final String CLIENT_ID = "webapp";

    // ==================== normalizeApiPrefix ====================

    @ParameterizedTest
    @ValueSource(strings = {"/xm-api", "xm-api", "/xm-api/", "xm-api/", "  /xm-api  "})
    void normalizeApiPrefix_bringsConfiguredValueToACanonicalPath(String configured) {
        assertEquals(API_PREFIX, normalizeApiPrefix(configured));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "/", "  /  "})
    void normalizeApiPrefix_treatsBlankAsNotConfigured(String configured) {
        assertNull(normalizeApiPrefix(configured));
    }

    // ==================== stripApiPrefix ====================

    @ParameterizedTest
    @CsvSource({
        "/xm-api/uaa/oauth/token, /uaa/oauth/token",
        "/xm-api/uaa,             /uaa",
        "/xm-api/,                /",
        "/xm-api,                 /"
    })
    void stripApiPrefix_removesTheLeadingPrefix(String requestUri, String expected) {
        assertEquals(expected, stripApiPrefix(requestUri, API_PREFIX));
    }

    /**
     * The prefix is optional: a request that arrives without it must keep working, so both forms are served
     * side by side.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "/uaa/oauth/token",
        "/xm-apix/uaa/oauth/token",
        "/uaa/xm-api/oauth/token",
        "/"
    })
    void stripApiPrefix_leavesEveryOtherPathUntouched(String requestUri) {
        assertEquals(requestUri, stripApiPrefix(requestUri, API_PREFIX));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "/"})
    void stripApiPrefix_isANoopWhenPrefixIsNotConfigured(String blankPrefix) {
        assertEquals("/xm-api/uaa/oauth/token", stripApiPrefix("/xm-api/uaa/oauth/token", blankPrefix));
    }

    @Test
    void stripApiPrefix_toleratesSloppyConfiguredPrefix() {
        assertEquals("/uaa/oauth/token", stripApiPrefix("/xm-api/uaa/oauth/token", "xm-api"));
        assertEquals("/uaa/oauth/token", stripApiPrefix("/xm-api/uaa/oauth/token", "/xm-api/"));
        assertEquals("/uaa/oauth/token", stripApiPrefix("/xm-api/uaa/oauth/token", "  /xm-api  "));
    }

    @Test
    void stripApiPrefix_nullUriStaysNull() {
        assertNull(stripApiPrefix(null, API_PREFIX));
    }

    // ==================== extractServiceName ====================

    /**
     * Guards the 401 regression: with the prefix still in place the first segment resolves to "xm-api", which is
     * not a registered service, so {@code AccessControlAuthorizationManager} denied the request. The prefix is
     * removed by {@code ApiPrefixStrippingFilter} before this runs.
     */
    @ParameterizedTest
    @CsvSource({
        "/uaa/oauth/token,                               uaa",
        "/config/api/profile/webapp/settings-public.yml,  config",
        "/entity,                                        entity"
    })
    void extractServiceName_readsTheFirstSegmentOfAStrippedPath(String requestUri, String expectedServiceName) {
        assertEquals(expectedServiceName, extractServiceName(stripApiPrefix(API_PREFIX + requestUri, API_PREFIX)));
        assertEquals(expectedServiceName, extractServiceName(stripApiPrefix(requestUri, API_PREFIX)));
    }

    @Test
    void extractServiceName_ofPrefixAloneHasNoService() {
        assertNull(extractServiceName(stripApiPrefix("/xm-api", API_PREFIX)));
        assertNull(extractServiceName(stripApiPrefix("/xm-api/", API_PREFIX)));
    }

    // ==================== getClientIdFromToken ====================

    /**
     * Guards XM1-36738: an expired token failed claim parsing, the rate-limit filter got a RuntimeException and the
     * gate answered 500 instead of letting the downstream service reply 401.
     */
    @Test
    void getClientIdFromToken_readsExpiredToken() throws JoseException {
        NumericDate expiredTwoDaysAgo = NumericDate.now();
        expiredTwoDaysAgo.addSeconds(-2 * 24 * 60 * 60);

        assertEquals(CLIENT_ID, getClientIdFromToken(requestWithBearer(signedToken(expiredTwoDaysAgo))));
    }

    @Test
    void getClientIdFromToken_readsValidToken() throws JoseException {
        NumericDate inOneHour = NumericDate.now();
        inOneHour.addSeconds(60 * 60);

        assertEquals(CLIENT_ID, getClientIdFromToken(requestWithBearer(signedToken(inOneHour))));
    }

    @Test
    void getClientIdFromToken_isNullForMalformedToken() {
        assertNull(getClientIdFromToken(requestWithBearer("not-a-jwt")));
    }

    @Test
    void getClientIdFromToken_isNullWithoutToken() {
        assertNull(getClientIdFromToken(new MockHttpServletRequest()));
    }

    private static MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AUTHORIZATION, "Bearer " + token);
        return request;
    }

    private static String signedToken(NumericDate expiration) throws JoseException {
        JwtClaims claims = new JwtClaims();
        claims.setClaim("client_id", CLIENT_ID);
        claims.setExpirationTime(expiration);

        RsaJsonWebKey key = RsaJwkGenerator.generateJwk(2048);
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(key.getPrivateKey());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        return jws.getCompactSerialization();
    }
}
