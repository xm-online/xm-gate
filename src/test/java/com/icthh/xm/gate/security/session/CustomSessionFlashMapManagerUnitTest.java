package com.icthh.xm.gate.security.session;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.FlashMap;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
        when(httpServletRequest.getSession(false))
            .thenThrow(new IllegalStateException())
            .thenReturn(null);
        List<FlashMap> flashMaps = customSessionFlashMapManager.retrieveFlashMaps(httpServletRequest);
        Assertions.assertNull(flashMaps);
    }
}
