package com.icthh.xm.gate;

import com.icthh.xm.gate.config.AsyncSyncConfiguration;
import com.icthh.xm.gate.config.TestSecurityConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = { GateApp.class, TestSecurityConfiguration.class, AsyncSyncConfiguration.class }
)
public @interface IntegrationTest {}
