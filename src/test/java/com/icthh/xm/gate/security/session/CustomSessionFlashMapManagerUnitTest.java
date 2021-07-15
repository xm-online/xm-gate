package com.icthh.xm.gate.security.session;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.FlashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.icthh.xm.gate.config.Constants.JSESSIONID_COOKIE_NAME;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomSessionFlashMapManagerUnitTest {

    private final CustomSessionFlashMapManager customSessionFlashMapManager = new CustomSessionFlashMapManager();
    @Mock
    private final HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test_shouldHandleException() {
        MockHttpSession mockHttpSession = new MockHttpSession();
        Cookie[] cookies = Arrays.array(new Cookie(JSESSIONID_COOKIE_NAME, "f95808d8-254a-40c7-9561-b1b4fe001307"));

        when(httpServletRequest.getSession(false))
            .thenThrow(new IllegalStateException())
            .thenReturn(mockHttpSession);

        when(httpServletRequest.getCookies())
            .thenReturn(cookies);

        List<FlashMap> flashMaps = customSessionFlashMapManager.retrieveFlashMaps(httpServletRequest);

        Assertions.assertNull(flashMaps);
    }
}
