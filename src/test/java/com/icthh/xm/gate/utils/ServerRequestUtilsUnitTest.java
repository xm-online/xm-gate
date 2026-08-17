package com.icthh.xm.gate.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.icthh.xm.gate.utils.ServerRequestUtils.extractServiceName;
import static com.icthh.xm.gate.utils.ServerRequestUtils.normalizeApiPrefix;
import static com.icthh.xm.gate.utils.ServerRequestUtils.stripApiPrefix;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServerRequestUtilsUnitTest {

    private static final String API_PREFIX = "/xm-api";

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
}
