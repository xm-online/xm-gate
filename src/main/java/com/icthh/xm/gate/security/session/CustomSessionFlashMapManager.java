package com.icthh.xm.gate.security.session;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import java.util.List;

import static com.icthh.xm.gate.config.Constants.JSESSIONID_COOKIE_NAME;

/**
 * Custom FlashMapManager that handles invalid session errors gracefully.
 * Extension point for sharing session data between cluster instances.
 */
@Slf4j
public class CustomSessionFlashMapManager extends SessionFlashMapManager {

    @Override
    protected @Nullable List<FlashMap> retrieveFlashMaps(HttpServletRequest request) {
        try {
            return super.retrieveFlashMaps(request);
        } catch (IllegalStateException e) {
            handleInvalidSession(request, e);
            return null;
        }
    }

    private void handleInvalidSession(HttpServletRequest request, IllegalStateException e) {
        log.error("Handling session error: {}", e.getMessage());
        SecurityContextHolder.clearContext();
        logSessionInfo(request);
        logCookies(request);
        performLogout(request);
    }

    private void logSessionInfo(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.warn("Session with JSESSIONID '{}' invalid.", session.getId());
        }
    }

    private void logCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                log.warn("Existing cookie name '{}', value '{}', maxage '{}'",
                    cookie.getName(), cookie.getValue(), cookie.getMaxAge());
            }
        }
    }

    private void performLogout(HttpServletRequest request) {
        log.warn("Clearing cookie [{}] and perform logout.", JSESSIONID_COOKIE_NAME);
        try {
            request.logout();
        } catch (ServletException e) {
            log.error("Failed to perform logout", e);
        }
    }
}
