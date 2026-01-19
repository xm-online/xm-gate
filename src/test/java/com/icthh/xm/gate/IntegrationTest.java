package com.icthh.xm.gate;

import com.icthh.xm.gate.config.AsyncSyncConfiguration;
import com.icthh.xm.gate.config.JacksonConfiguration;
import com.icthh.xm.gate.config.TestSecurityConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = { XmGateJava25App.class, JacksonConfiguration.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class }
)
public @interface IntegrationTest {}
