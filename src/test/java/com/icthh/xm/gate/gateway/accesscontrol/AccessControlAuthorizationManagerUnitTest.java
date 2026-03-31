package com.icthh.xm.gate.gateway.accesscontrol;

import com.icthh.xm.gate.config.properties.ApplicationProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.List;
import java.util.function.Supplier;

import static com.icthh.xm.gate.gateway.accesscontrol.RuleBuilder.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlAuthorizationManagerUnitTest {

    @Mock
    private DiscoveryClient discoveryClient;
    @Mock
    private ApplicationProperties appProperties;
    @Mock
    private ApplicationProperties.Gateway gateway;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private RequestAuthorizationContext ctx;

    private AccessControlAuthorizationManager manager;

    @BeforeEach
    void setUp() {
        manager = new AccessControlAuthorizationManager(discoveryClient, appProperties);
        when(ctx.getRequest()).thenReturn(servletRequest);
        when(appProperties.getGateway()).thenReturn(gateway);
    }

    @Test
    void permitAll_rule_allows_unauthenticated_request() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/public/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of(
            rule("/my-service/public/**").permitAll()
        ));

        assertTrue(isGranted(manager.authorize(unauthenticated(), ctx)));
    }

    @Test
    void authenticated_rule_allows_authenticated_request() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/api/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of(
            rule("/my-service/api/**").authenticated()
        ));

        assertTrue(isGranted(manager.authorize(authenticated(), ctx)));
    }

    @Test
    void authenticated_rule_denies_unauthenticated_request() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/api/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of(
            rule("/my-service/api/**").authenticated()
        ));

        assertFalse(isGranted(manager.authorize(unauthenticated(), ctx)));
    }

    @Test
    void authority_rule_allows_when_authentication_has_required_authority() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/management/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of(
            rule("/my-service/management/**").authorities("ROLE_ADMIN")
        ));

        assertTrue(isGranted(manager.authorize(authenticatedWith("ROLE_ADMIN"), ctx)));
    }

    @Test
    void authority_rule_denies_when_authentication_missing_required_authority() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/management/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of(
            rule("/my-service/management/**").authorities("ROLE_ADMIN")
        ));

        assertFalse(isGranted(manager.authorize(authenticatedWith("ROLE_USER"), ctx)));
    }

    @Test
    void authority_rule_denies_unauthenticated_request() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/management/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of(
            rule("/my-service/management/**").authorities("ROLE_ADMIN")
        ));

        assertFalse(isGranted(manager.authorize(unauthenticated(), ctx)));
    }

    @Test
    void authority_rule_allows_when_one_of_multiple_authorities_matches() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/management/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of(
            rule("/my-service/management/**").authorities("ROLE_ADMIN", "ROLE_SUPERUSER")
        ));

        assertTrue(isGranted(manager.authorize(authenticatedWith("ROLE_SUPERUSER"), ctx)));
    }

    @Test
    void first_matching_rule_wins_deny_before_later_permitAll() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/admin/secret");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of(
            rule("/my-service/admin/**").authorities("ROLE_ADMIN"),
            rule("/**").permitAll()
        ));

        // ROLE_USER matches the first rule but lacks ROLE_ADMIN → denied
        // the permitAll rule is never reached
        assertFalse(isGranted(manager.authorize(authenticatedWith("ROLE_USER"), ctx)));
    }

    @Test
    void no_matching_rule_falls_through_to_service_discovery() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/api/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of(
            rule("/other-service/**").permitAll()
        ));
        when(discoveryClient.getServices()).thenReturn(List.of("my-service"));

        assertTrue(isGranted(manager.authorize(unauthenticated(), ctx)));
    }

    @Test
    void empty_rules_fall_through_and_allow_registered_service() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/api/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of());
        when(discoveryClient.getServices()).thenReturn(List.of("my-service"));

        assertTrue(isGranted(manager.authorize(unauthenticated(), ctx)));
    }

    @Test
    void empty_rules_fall_through_and_deny_unregistered_service() {
        when(servletRequest.getRequestURI()).thenReturn("/unknown/api/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of());
        when(discoveryClient.getServices()).thenReturn(List.of("my-service"));

        assertFalse(isGranted(manager.authorize(unauthenticated(), ctx)));
    }

    @Test
    void allows_registered_service() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/api/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of());
        when(discoveryClient.getServices()).thenReturn(List.of("my-service"));

        assertTrue(isGranted(manager.authorize(unauthenticated(), ctx)));
    }

    @Test
    void denies_unregistered_service() {
        when(servletRequest.getRequestURI()).thenReturn("/unknown-service/api/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of());
        when(discoveryClient.getServices()).thenReturn(List.of("my-service"));

        assertFalse(isGranted(manager.authorize(unauthenticated(), ctx)));
    }

    @Test
    void service_name_matching_is_case_insensitive() {
        when(servletRequest.getRequestURI()).thenReturn("/MY-SERVICE/api/data");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of());
        when(discoveryClient.getServices()).thenReturn(List.of("my-service"));

        assertTrue(isGranted(manager.authorize(unauthenticated(), ctx)));
    }

    @Test
    void deny_unauthorized_path_for_registered_service() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/admin/secret");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of(
            rule("/my-service/admin/**").authorities("ROLE_ADMIN")
        ));
        verifyNoInteractions(discoveryClient);

        assertFalse(isGranted(manager.authorize(authenticatedWith("ROLE_USER"), ctx)));
    }

    @Test
    void allow_authorized_path_for_registered_service() {
        when(servletRequest.getRequestURI()).thenReturn("/my-service/admin/secret");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of(
            rule("/my-service/admin/**").authorities("ROLE_ADMIN")
        ));
        verifyNoInteractions(discoveryClient);

        assertTrue(isGranted(manager.authorize(authenticatedWith("ROLE_ADMIN"), ctx)));
    }

    @Test
    void denies_when_service_name_cannot_be_extracted() {
        when(servletRequest.getRequestURI()).thenReturn("/");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of());

        assertFalse(isGranted(manager.authorize(unauthenticated(), ctx)));
    }

    @Test
    void denies_unauthenticated_unconfigured_request() {
        when(servletRequest.getRequestURI()).thenReturn("/api/admin/secret");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of());
        when(discoveryClient.getServices()).thenReturn(List.of());

        assertFalse(isGranted(manager.authorize(unauthenticated(), ctx)));
    }

    @Test
    void allow_authenticated_unconfigured_request() {
        when(servletRequest.getRequestURI()).thenReturn("/api/admin/secret");
        when(gateway.getAuthRequestMatcherRules()).thenReturn(List.of());
        when(discoveryClient.getServices()).thenReturn(List.of());

        assertTrue(isGranted(manager.authorize(authenticated(), ctx)));
    }

    private static boolean isGranted(AuthorizationResult result) {
        return result instanceof AuthorizationDecision d && d.isGranted();
    }

    private static Supplier<Authentication> unauthenticated() {
        return () -> null;
    }

    private static Supplier<Authentication> authenticated() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        return () -> auth;
    }

    private static Supplier<Authentication> authenticatedWith(String... authorities) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getAuthorities()).thenAnswer(_ ->
            java.util.Arrays.stream(authorities)
                .<GrantedAuthority>map(a -> () -> a)
                .toList()
        );
        return () -> auth;
    }
}
