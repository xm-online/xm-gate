package com.icthh.xm.gate.utils.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class CookieUtils {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        return Optional.ofNullable(request.getCookies())
            .stream()
            .flatMap(Arrays::stream)
            .filter(Objects::nonNull)
            .filter(cookie -> name.equals(cookie.getName()))
            .findFirst();
    }

    public static void addCookie(HttpServletRequest request, HttpServletResponse response,
                                 String name, String value, int maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
            .path("/")
            .httpOnly(true)
            .secure(request.isSecure())
            .sameSite("Lax")
            .maxAge(Duration.ofSeconds(maxAge))
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        if (getCookie(request, name).isPresent()) {
            addCookie(request, response, name, "", 0);
        }
    }
}
