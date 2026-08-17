package com.icthh.xm.gate.gateway.filter;

import com.icthh.xm.gate.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End to end check of the optional api prefix (see {@code application.gateway.api-prefix}) through the real
 * filter chain: {@code ApiPrefixStrippingFilter}, the Spring Security chain and the {@code DispatcherServlet}.
 */
@IntegrationTest
@AutoConfigureMockMvc
class ApiPrefixIntTest {

    private static final String API_PREFIX = "/xm-api";

    @Autowired
    private MockMvc mockMvc;

    /**
     * The gate's own endpoints are mapped by Spring MVC on their plain paths, so reaching them under the prefix
     * proves the path is rewritten before handler mapping, not only before the security matchers.
     */
    @Test
    void health_isReachedWithAndWithoutApiPrefix() throws Exception {
        int plain = getStatus("/management/health");
        int prefixed = getStatus(API_PREFIX + "/management/health");

        // the actual status depends on the health indicators, what matters is that the handler is reached at all
        assertNotEquals(404, plain, "precondition: the actuator endpoint is mapped");
        assertEquals(plain, prefixed, "the api prefix must not change how /management/health is handled");
    }

    /**
     * The regression: a prefixed path used to miss the {@code /*&#47;oauth/**} permit-all rule, resolve to the
     * unknown service "xm-api" and get 401, while the very same plain path was served.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/uaa/oauth/token", "/uaa/public/anything", "/uaa/api/public/anything"})
    void permittedProxiedPath_answersTheSameWithAndWithoutApiPrefix(String path) throws Exception {
        int plain = postStatus(path);
        int prefixed = postStatus(API_PREFIX + path);

        assertNotEquals(401, plain, "precondition: the plain path is permitted by security");
        assertEquals(plain, prefixed, "the api prefix must not change how " + path + " is authorized");
    }

    /**
     * Stripping must not hand out access: a path that needs an authority stays refused under the prefix too.
     */
    @Test
    void protectedPath_staysRefusedWithAndWithoutApiPrefix() throws Exception {
        int plain = getStatus("/management/loggers");
        int prefixed = getStatus(API_PREFIX + "/management/loggers");

        assertEquals(plain, prefixed, "the api prefix must not change how /management/loggers is authorized");
        assertNotEquals(200, plain, "precondition: /management/loggers is not public");
    }

    /**
     * The prefix is optional - a lookalike first segment is a normal path and must not be stripped.
     */
    @Test
    void prefixLookalike_isNotTreatedAsThePrefix() throws Exception {
        assertNotEquals(getStatus("/management/health"), getStatus("/xm-apix/management/health"));
    }

    private int getStatus(String path) throws Exception {
        return mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
    }

    private int postStatus(String path) throws Exception {
        return mockMvc.perform(post(path)).andReturn().getResponse().getStatus();
    }
}
