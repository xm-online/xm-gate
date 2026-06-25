package com.icthh.xm.gate.gateway.accesscontrol;

import com.icthh.xm.gate.config.properties.ApplicationProperties;
import com.icthh.xm.gate.config.properties.ApplicationProperties.AuthRequestMatcherRule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.icthh.xm.gate.utils.ServerRequestUtils.extractServiceName;
import static org.apache.commons.lang3.StringUtils.EMPTY;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessControlAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private static final AuthorizationDecision DENY = new AuthorizationDecision(false);
    private static final AuthorizationDecision ALLOW = new AuthorizationDecision(true);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApplicationProperties appProperties;

    @Override
    public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
                                                   RequestAuthorizationContext ctx) {
        String requestUri = getRequestUri(ctx);

        Boolean isAuthorizedByRule = isAuthorizedByRule(authentication, requestUri);
        if (isAuthorizedByRule != null) {
            return isAuthorizedByRule ? ALLOW : DENY;
        }

        String serviceName = extractServiceName(requestUri);
        if (StringUtils.isBlank(serviceName)) {
            log.warn("Access Control: could not determine service name for {}", requestUri);
            return DENY;
        }

        if (isRegisteredService(serviceName)) {
            log.debug("Access Control: registered service requested: {}", serviceName);
            return ALLOW;
        }
        log.debug("Access Control: registered service requested: {}", serviceName);
        Authentication auth = authentication.get();
        return isAuthenticated(auth) ? ALLOW : DENY;
    }

    private @Nullable Boolean isAuthorizedByRule(Supplier<? extends @Nullable Authentication> authentication,
                                                 String requestUri) {
        List<AuthRequestMatcherRule> rules = appProperties.getGateway().getAuthRequestMatcherRules();

        if (rules.isEmpty()) {
            log.info("Access Control: access control policy has not been configured");
            return null;
        }
        return rules.stream()
            .filter(r -> PATH_MATCHER.match(r.getPathPattern(), requestUri))
            .findFirst()
            .map(rule -> evaluateRule(authentication, rule))
            .orElse(null);
    }

    private boolean evaluateRule(Supplier<? extends @Nullable Authentication> authentication,
                                 AuthRequestMatcherRule rule) {
        if (rule.isPermitAll()) {
            log.info("Access Control: allow access, due to permit-all rule");
            return true;
        }
        Authentication auth = authentication.get();
        boolean authenticated = isAuthenticated(auth);
        if (rule.getAuthorities() != null && rule.getAuthorities().length > 0) {
            boolean granted = authenticated && hasAuthority(auth, rule.getAuthorities());
            log.info("Access Control: {} access", granted ? "authorized" : "unauthorized");
            return granted;
        }
        return authenticated;
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    private boolean hasAuthority(Authentication auth, String[] required) {
        if (auth == null) {
            return false;
        }
        var userAuthorities = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
        return Arrays.stream(required).anyMatch(userAuthorities::contains);
    }

    private boolean isRegisteredService(String serviceName) {
        return appProperties.getGateway().getXmeRoutes().stream()
            .anyMatch(route -> route.equalsIgnoreCase(serviceName));
    }

    private String getRequestUri(RequestAuthorizationContext ctx) {
        return Optional.ofNullable(ctx)
            .map(RequestAuthorizationContext::getRequest)
            .map(HttpServletRequest::getRequestURI)
            .orElse(EMPTY);
    }
}
