package com.icthh.xm.gate.gateway.accesscontrol;

import com.icthh.xm.gate.config.properties.ApplicationProperties;
import com.icthh.xm.gate.config.properties.ApplicationProperties.AuthRequestMatcherRule;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.cloud.client.discovery.DiscoveryClient;
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

    private final DiscoveryClient discoveryClient;
    private final ApplicationProperties appProperties;

    @Override
    public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
                                                   RequestAuthorizationContext ctx) {
        String requestUri = getRequestUri(ctx);
        Authentication auth = authentication.get();

        Boolean isAuthorizedByRule = isAuthorizedByRule(auth, requestUri);
        if (isAuthorizedByRule != null) {
            return new AuthorizationDecision(isAuthorizedByRule);
        }

        String serviceName = extractServiceName(requestUri);
        if (StringUtils.isBlank(serviceName)) {
            log.info("Access Control: could not determine service name for {}", requestUri);
            return DENY;
        }

        if (isRegisteredService(serviceName)) {
            log.warn("Access Control: registered service requested: {}", serviceName);
            return ALLOW;
        }
        return new AuthorizationDecision(isAuthenticated(auth));
    }

    private Boolean isAuthorizedByRule(@Nullable Authentication authentication, String requestUri) {
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

    private boolean evaluateRule(Authentication auth, AuthRequestMatcherRule rule) {
        if (rule.isPermitAll()) {
            log.info("Access Control: allow access, due to permit-all rule");
            return true;
        }
        boolean authenticated = isAuthenticated(auth);
        if (rule.getAuthorities() != null && rule.getAuthorities().length > 0) {
            boolean granted = authenticated && hasAuthority(auth, rule.getAuthorities());
            log.info("Access Control: {} access", granted ? "authorized" : "unauthorized");
            return granted;
        }
        return authenticated;
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated();
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
        return discoveryClient.getServices()
            .stream()
            .anyMatch(s -> s.equalsIgnoreCase(serviceName));
    }

    private String getRequestUri(RequestAuthorizationContext ctx) {
        return Optional.ofNullable(ctx)
            .map(RequestAuthorizationContext::getRequest)
            .map(HttpServletRequest::getRequestURI)
            .orElse(EMPTY);
    }
}
