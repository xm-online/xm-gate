package com.icthh.xm.gate.config;

import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WebMvcTest
@ExtendWith(SpringExtension.class)
@Import(LocaleConfiguration.class)
@ContextConfiguration(classes = {LocaleConfigurationUnitTest.TestController.class})
public class LocaleConfigurationUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldResolveLocaleFromCookieAndRemoveQuotes() throws Exception {
        mockMvc.perform(get("/test")
                .cookie(new Cookie("NG_TRANSLATE_LANG_KEY", "%22uk%22")))
            .andExpect(status().isOk())
            .andExpect(content().string("uk"));
    }

    @Test
    void shouldResolveLocaleFromQuotedValue() throws Exception {
        mockMvc.perform(get("/test")
                .cookie(new Cookie("NG_TRANSLATE_LANG_KEY", "\"uk\"")))
            .andExpect(status().isOk())
            .andExpect(content().string("uk"));
    }

    @Test
    void shouldChangeLocaleUsingRequestParam() throws Exception {
        mockMvc.perform(get("/test")
                .param("language", "uk"))
            .andExpect(status().isOk())
            .andExpect(content().string("uk"));
    }

    @RestController
    static class TestController {
        @GetMapping("/test")
        public String test(Locale locale) {
            return locale.getLanguage();
        }
    }
}
