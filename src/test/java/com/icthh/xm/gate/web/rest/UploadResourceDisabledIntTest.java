package com.icthh.xm.gate.web.rest;

import com.icthh.xm.gate.IntegrationTest;
import com.icthh.xm.gate.web.rest.file.UploadResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deprecated upload proxy is disabled unless application.enable-upload-resource=true.
 */
@IntegrationTest
class UploadResourceDisabledIntTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void uploadResourceIsDisabledByDefault() {
        assertThat(applicationContext.getBeanNamesForType(UploadResource.class)).isEmpty();
    }
}
