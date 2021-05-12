package com.icthh.xm.gate.security.session;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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
        } catch (IllegalStateException illegalStateException) {
            log.error("{}", illegalStateException.getMessage());

            SecurityContextHolder.clearContext();

            HttpSession session = servletRequest.getSession(false);

            if (session != null) {
                log.warn("Session with JSESSIONID '{}' invalid.", session.getId());
                log.warn("Session '{}' created at '{}'", session.getId(), convertToDate(session.getCreationTime()));
                log.warn("Session '{}' last accessed at '{}'", session.getId(), convertToDate(session.getLastAccessedTime()));
                List.of(servletRequest.getCookies()).forEach(cookie -> {
                    log.warn("Session cookie name '{}', value '{}', maxage '{}'",
                        cookie.getName(), cookie.getValue(), cookie.getMaxAge());
                });
            }
            log.warn("Clearing cookies and perform logout.");
            servletRequest.logout();
        }
        return null;
    }

    private String convertToDate(long timeStamp) {
        Date date = new Date(timeStamp);
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        return formatter.format(date);
    }
}
