package com.icthh.xm.gate.web.rest;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.LoggerContext;
import com.icthh.xm.commons.logging.web.rest.LogsResource;
import com.icthh.xm.gate.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the LogsResource REST controller.
 *
 * @see LogsResource
 */
@IntegrationTest
public class LogsResourceIntTest {

    private MockMvc restLogsMockMvc;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);

        LogsResource logsResource = new LogsResource();
        this.restLogsMockMvc = MockMvcBuilders
            .standaloneSetup(logsResource)
            .build();
    }

    @Test
    public void getAllLogs()throws Exception {
        restLogsMockMvc.perform(get("/management/logs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    public void changeLogs()throws Exception {
        restLogsMockMvc.perform(put("/management/logs")
            .contentType(new MediaType(MediaType.APPLICATION_JSON, UTF_8))
            .content("{\"level\":\"INFO\", \"name\": \"ROOT\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    public void testLogstashAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        assertThat(context.getLogger("ROOT").getAppender("ASYNC_LOGSTASH")).isInstanceOf(AsyncAppender.class);
    }
}
