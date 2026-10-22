package com.icthh.xm.gate.config;

import com.icthh.xm.gate.IntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The config service private api ({@code /config/api/private/**}) is not served through the gate,
 * with or without the api prefix.
 */
@IntegrationTest
@AutoConfigureMockMvc
class ConfigPrivateApiIntTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
        "/config/api/private/config_map",
        "/config/api/private/config_map/pattern",
        "/xm-api/config/api/private/config_map",
        "/config/api/private/config_map/anonymous/"
    })
    void anonymousRequest_isRejected(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(put(path)).andExpect(status().isUnauthorized());
    }
}
