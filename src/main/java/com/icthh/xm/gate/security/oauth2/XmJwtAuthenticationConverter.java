package com.icthh.xm.gate.security.oauth2;

import com.icthh.xm.commons.security.jwt.TokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class XmJwtAuthenticationConverter implements AuthenticationConverter {

    private final TokenProvider tokenProvider;

    private static final BearerTokenResolver tokenResolver = new DefaultBearerTokenResolver();

    @Override
    public @Nullable Authentication convert(HttpServletRequest request) {
        String jwt = tokenResolver.resolve(request);
        if (StringUtils.hasText(jwt) && this.tokenProvider.validateToken(jwt)) {
            Authentication authentication = this.tokenProvider.getAuthentication(request, jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        return null;
    }
}
