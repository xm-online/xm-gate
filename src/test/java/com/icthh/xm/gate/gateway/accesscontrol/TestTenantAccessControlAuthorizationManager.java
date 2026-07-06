package com.icthh.xm.gate.gateway.accesscontrol;

import com.icthh.xm.gate.config.properties.ApplicationProperties;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

public class TestTenantAccessControlAuthorizationManager extends AccessControlAuthorizationManager{
    public TestTenantAccessControlAuthorizationManager(ApplicationProperties appProperties) {
        super(appProperties);
    }

    @Override
    protected @Nullable Boolean isAuthorizedByTenantRule(Supplier<? extends @Nullable Authentication> authentication, String requestUri) {
        return requestUri != null && requestUri.startsWith("/TEST/public/");
    }
}
