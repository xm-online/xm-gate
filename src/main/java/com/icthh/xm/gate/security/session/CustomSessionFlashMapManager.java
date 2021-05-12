package com.icthh.xm.gate.security.session;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

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
            servletRequest.logout();
        }
        return null;
    }
}
