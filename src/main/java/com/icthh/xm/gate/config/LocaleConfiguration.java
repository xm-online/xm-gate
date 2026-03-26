package com.icthh.xm.gate.config;

import com.icthh.xm.commons.i18n.spring.config.CommonMessageSourceConfiguration;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.util.WebUtils;

import java.util.Locale;

import static org.apache.logging.log4j.util.Strings.EMPTY;
import static org.springframework.util.StringUtils.replace;

@Configuration
@Import({CommonMessageSourceConfiguration.class})
public class LocaleConfiguration implements WebMvcConfigurer {

    @Bean(name = "localeResolver")
    public LocaleResolver localeResolver() {
        return new CustomCookieLocaleResolver();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("language");
        registry.addInterceptor(localeChangeInterceptor);
    }

    private static class CustomCookieLocaleResolver extends CookieLocaleResolver {

        private static final String QUOTE = "%22";
        private static final String COOKIE_NAME = "NG_TRANSLATE_LANG_KEY";

        public CustomCookieLocaleResolver() {
            super(COOKIE_NAME);
        }

        @Override
        public LocaleContext resolveLocaleContext(HttpServletRequest request) {
            HttpServletRequest normalizedRequest = normalizeLocaleCookie(request);
            return super.resolveLocaleContext(normalizedRequest);
        }

        @Override
        public Locale resolveLocale(HttpServletRequest request) {
            HttpServletRequest normalizedRequest = normalizeLocaleCookie(request);
            return super.resolveLocale(normalizedRequest);
        }

        private HttpServletRequest normalizeLocaleCookie(HttpServletRequest request) {
            Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);
            if (cookie != null) {
                String lang = cookie.getValue();
                lang = replace(lang, QUOTE, EMPTY);
                lang = replace(lang, "\"", EMPTY);
                cookie.setValue(lang);
            }
            return request;
        }
    }
}
