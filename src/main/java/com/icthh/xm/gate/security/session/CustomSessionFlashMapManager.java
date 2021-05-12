package com.icthh.xm.gate.security.session;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
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

            HttpSession session = servletRequest.getSession(false);
            log.warn("Session with JSESSIONID {} invalid. Clearing cookies and perform logout.", session.getId());
            log.warn("Session created at '{}'", convertToDate(session.getCreationTime()));
            log.warn("Session last accessed at '{}'", convertToDate(session.getLastAccessedTime()));
            List.of(servletRequest.getCookies()).forEach(cookie -> {
                log.warn("Session cookie name '{}', value '{}', maxage '{}'",
                    cookie.getName(), cookie.getValue(), cookie.getMaxAge());
            });

            servletRequest.logout();
        }
        return null;
    }

    private String convertToDate(long timeStamp) {
        Date date = new Date(timeStamp);
        DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(date);
    }
}
