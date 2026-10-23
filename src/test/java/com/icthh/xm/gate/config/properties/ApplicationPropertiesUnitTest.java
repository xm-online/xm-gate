package com.icthh.xm.gate.config.properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApplicationPropertiesUnitTest {

    @Test
    public void testManagementDefaults() {
        ApplicationProperties properties = bind(Map.of("application.kafka-enabled", "false"));

        assertFalse(properties.getPrometheus().isPublic());
        assertEquals("SYSTEM-ADMIN", properties.getManagement().getAccess().getRole());
    }

    @Test
    public void testManagementPropertiesAreBound() {
        ApplicationProperties properties = bind(Map.of(
            "application.prometheus.public", "true",
            "application.management.access.role", "SUPER-ADMIN"));

        assertTrue(properties.getPrometheus().isPublic());
        assertEquals("SUPER-ADMIN", properties.getManagement().getAccess().getRole());
    }

    private static ApplicationProperties bind(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
            .bind("application", ApplicationProperties.class)
            .get();
    }
}
