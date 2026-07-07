package com.icthh.xm.gate.gateway.accesscontrol;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

public interface TenantAuthorizationRule {

    Boolean authorize(Supplier<? extends @Nullable Authentication> authentication, String requestUri);
}
