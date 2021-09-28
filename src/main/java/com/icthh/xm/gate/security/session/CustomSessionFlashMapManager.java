package com.icthh.xm.gate.security.session;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.icthh.xm.gate.config.Constants.JSESSIONID_COOKIE_NAME;

@Slf4j
public class CustomSessionFlashMapManager extends SessionFlashMapManager {

    public CustomSessionFlashMapManager() {
    }

    //TODO Extension point for sharing session data between cluster instances
    @SneakyThrows
    @Override
    @Nullable
    protected List<FlashMap> retrieveFlashMaps(HttpServletRequest servletRequest) {
        try {
            return super.retrieveFlashMaps(servletRequest);
        } catch (IllegalStateException e) {
            log.error("Handling session error: {}", e.getMessage());

            HttpSession session = servletRequest.getSession(false);

            if (session != null) {
                log.warn("Session with JSESSIONID '{}' invalid.", session.getId());
            }

            Optional.ofNullable(servletRequest.getCookies())
                .stream()
                .flatMap(Arrays::stream)
                .forEach(cookie -> {
                    log.warn("Existing cookie name '{}', value '{}', maxage '{}'",
                        cookie.getName(), cookie.getValue(), cookie.getMaxAge());
                });

            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (requestAttributes != null && requestAttributes.getResponse() != null) {
                log.warn("Clearing cookie [" + JSESSIONID_COOKIE_NAME + "] and perform logout.");
                CookieClearingLogoutHandler clearingLogoutHandler = new CookieClearingLogoutHandler(JSESSIONID_COOKIE_NAME);

                HttpServletResponse httpServletResponse = requestAttributes.getResponse();
                httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                clearingLogoutHandler.logout(servletRequest, httpServletResponse, SecurityContextHolder.getContext().getAuthentication());
                SecurityContextHolder.getContext().setAuthentication(null);
                SecurityContextHolder.clearContext();
            }

            return null;
        }
    }
}
