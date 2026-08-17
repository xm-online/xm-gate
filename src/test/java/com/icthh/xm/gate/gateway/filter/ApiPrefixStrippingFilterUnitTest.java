package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.gate.config.properties.ApplicationProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.AntPathMatcher;

import static com.icthh.xm.gate.utils.ServerRequestUtils.extractServiceName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApiPrefixStrippingFilterUnitTest {

    private static final String API_PREFIX = "/xm-api";

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setMethod("POST");
        request.setServerName("tenant.ee-test.xm-online.com");
    }

    // ==================== stripping ====================

    @Test
    void prefixedRequest_isStripped() throws Exception {
        setPath("/xm-api/uaa/oauth/token");

        HttpServletRequest forwarded = filterAndCapture(filter(API_PREFIX));

        assertEquals("/uaa/oauth/token", forwarded.getRequestURI());
        assertEquals("/uaa/oauth/token", forwarded.getServletPath());
        assertEquals("http://tenant.ee-test.xm-online.com/uaa/oauth/token", forwarded.getRequestURL().toString());
    }

    /** the gate's own endpoints are mapped by Spring MVC on their plain paths, so they need stripping too */
    @ParameterizedTest
    @CsvSource({
        "/xm-api/management/health,     /management/health",
        "/xm-api/management/prometheus, /management/prometheus",
        "/xm-api/api/profile-info,      /api/profile-info",
        "/xm-api/oauth2/authorization/x,/oauth2/authorization/x"
    })
    void prefixedGateOwnEndpoint_isStripped(String requestUri, String expectedUri) throws Exception {
        setPath(requestUri);

        assertEquals(expectedUri, filterAndCapture(filter(API_PREFIX)).getRequestURI());
    }

    @ParameterizedTest
    @CsvSource({"/xm-api, /", "/xm-api/, /"})
    void prefixAlone_becomesRoot(String requestUri, String expectedUri) throws Exception {
        setPath(requestUri);

        assertEquals(expectedUri, filterAndCapture(filter(API_PREFIX)).getRequestURI());
    }

    @Test
    void contextPath_isPreserved() throws Exception {
        request.setContextPath("/gate");
        request.setRequestURI("/gate/xm-api/uaa/oauth/token");
        request.setServletPath("/xm-api/uaa/oauth/token");

        HttpServletRequest forwarded = filterAndCapture(filter(API_PREFIX));

        assertEquals("/gate/uaa/oauth/token", forwarded.getRequestURI());
        assertEquals("/uaa/oauth/token", forwarded.getServletPath());
    }

    @ParameterizedTest
    @ValueSource(strings = {"xm-api", "/xm-api/", "  /xm-api  "})
    void configuredPrefix_isNormalized(String configuredPrefix) throws Exception {
        setPath("/xm-api/uaa/oauth/token");

        assertEquals("/uaa/oauth/token", filterAndCapture(filter(configuredPrefix)).getRequestURI());
    }

    @Test
    void nonPathState_isDelegatedToTheOriginalRequest() throws Exception {
        setPath("/xm-api/uaa/oauth/token");
        request.setQueryString("grant_type=password");
        request.addHeader("Authorization", "Basic d2ViYXBwOndlYmFwcA==");
        request.setAttribute("attr", "value");

        HttpServletRequest forwarded = filterAndCapture(filter(API_PREFIX));

        assertEquals("grant_type=password", forwarded.getQueryString());
        assertEquals("Basic d2ViYXBwOndlYmFwcA==", forwarded.getHeader("Authorization"));
        assertEquals("value", forwarded.getAttribute("attr"));
        assertSame(request.getSession(true), forwarded.getSession(true));
    }

    // ==================== pass through ====================

    /**
     * The prefix is optional: while it is configured, a call made without it has to keep working, so both forms
     * run side by side.
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "/uaa/oauth/token",
        "/management/health",
        "/api/profile-info",
        "/xm-apix/uaa/oauth/token",
        "/uaa/xm-api/oauth/token",
        "/"
    })
    void nonPrefixedRequest_isPassedThroughUntouched(String path) throws Exception {
        setPath(path);

        filter(API_PREFIX).doFilterInternal(request, response, filterChain);

        // the very same instance, not a wrapper
        verify(filterChain).doFilter(request, response);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "/"})
    void blankPrefix_disablesStripping(String configuredPrefix) throws Exception {
        setPath("/xm-api/uaa/oauth/token");

        filter(configuredPrefix).doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    // ==================== downstream contract ====================

    /**
     * The regression this filter exists for: {@code /*} spans a single segment, so the permit-all rule of
     * {@code MicroserviceSecurityConfiguration} missed the prefixed token endpoint, the request fell through to
     * {@code anyRequest().access(...)}, where the service name resolved to "xm-api" - and the answer was 401.
     */
    @Test
    void strippedRequest_satisfiesTheDownstreamPathAssumptions() throws Exception {
        setPath("/xm-api/uaa/oauth/token");

        HttpServletRequest forwarded = filterAndCapture(filter(API_PREFIX));

        assertFalse(new AntPathMatcher().match("/*/oauth/**", request.getRequestURI()),
            "precondition: the prefixed path is the one that used to be rejected");
        assertTrue(new AntPathMatcher().match("/*/oauth/**", forwarded.getRequestURI()));
        assertEquals("uaa", extractServiceName(forwarded.getRequestURI()));
    }

    // ==================== helpers ====================

    private void setPath(String path) {
        request.setRequestURI(path);
        request.setServletPath(path);
    }

    private ApiPrefixStrippingFilter filter(String configuredPrefix) {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getGateway().setApiPrefix(configuredPrefix);
        return new ApiPrefixStrippingFilter(properties);
    }

    private HttpServletRequest filterAndCapture(ApiPrefixStrippingFilter filter) throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(captor.capture(), same(response));
        return captor.getValue();
    }
}
