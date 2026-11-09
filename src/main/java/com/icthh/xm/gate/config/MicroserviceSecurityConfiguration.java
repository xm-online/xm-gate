package com.icthh.xm.gate.config;

import com.icthh.xm.commons.security.XmPermissionCheckService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.gate.config.properties.ApplicationProperties;
import com.icthh.xm.gate.gateway.accesscontrol.AccessControlAuthorizationManager;
import com.icthh.xm.gate.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.icthh.xm.gate.security.oauth2.idp.IdpAuthenticationSuccessHandler;
import com.icthh.xm.gate.security.oauth2.idp.IdpClientRepository;
import com.icthh.xm.gate.security.oauth2.XmAuthorizationRequestResolver;
import com.icthh.xm.gate.security.oauth2.XmConfigServerService;
import com.icthh.xm.gate.security.oauth2.XmJwtDecoderFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.SingleResultAuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import static com.icthh.xm.commons.security.RoleConstant.ADMIN;
import static com.icthh.xm.commons.security.RoleConstant.SUPER_ADMIN;
import static com.icthh.xm.gate.config.Constants.JSESSIONID_COOKIE_NAME;
import static java.lang.Boolean.TRUE;
import static org.springframework.security.authorization.AuthorizationManagers.anyOf;

@Slf4j
@Configuration
@EnableMethodSecurity(securedEnabled = true)
@EnableWebSecurity
@RequiredArgsConstructor
public class MicroserviceSecurityConfiguration {

    private final XmConfigServerService xmConfigServerService;
    private final TenantContextHolder tenantContextHolder;
    private final IdpClientRepository idpClientRepository;
    private final IdpAuthenticationSuccessHandler idpSuccessHandler;
    private final ApplicationProperties applicationProperties;
    private final AccessControlAuthorizationManager authorizationManager;
    private final XmPermissionCheckService permissionCheckService;

    private static final String LOGS_GET_PRIVILEGE = "LOGS.GET";
    private static final String LOGS_UPDATE_PRIVILEGE = "LOGS.UPDATE";
    private static final String API_DOCS_PRIVILEGE = "API_DOCS.GET";

    @Value("${application.api-docs.privilege-check:true}")
    private boolean apiDocsPrivilegeCheck = true;

    private static final RequestMatcher ANONYMOUS_PATHS = request -> {
        String uri = request.getRequestURI();
        return uri != null && (uri.contains("/anonymous/") || uri.endsWith("/anonymous"));
    };

    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        ObjectProvider<AuthorizationRequestRepository<OAuth2AuthorizationRequest>> repositoryProvider) throws Exception {
        OAuth2AuthorizationRequestResolver resolver = requestResolver();
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> repository =
            repositoryProvider.getIfAvailable(this::defaultAuthorizationRequestRepository);
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            .logout(logout -> logout
                .logoutUrl("/")
                .addLogoutHandler(logoutHandler())
                .permitAll())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz ->
                // prettier-ignore
                authz
                    .requestMatchers("/config/api/private/**").denyAll()
                    // only authentication here, LOGS.GET / LOGS.UPDATE privileges are checked by the target microservice
                    .requestMatchers("/*/management/logs").access(routedLogsAccess())
                    .requestMatchers("/*/management/**").access(managementAccess())
                    .requestMatchers("/*/api/public/**").permitAll()
                    .requestMatchers(ANONYMOUS_PATHS).permitAll()
                    .requestMatchers("/oauth2/authorization/**").permitAll()
                    .requestMatchers("/login/oauth2/code/**").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .requestMatchers("/*/public/**").permitAll()
                    .requestMatchers("/*/oauth/**").permitAll()
                    .requestMatchers("/management/health").permitAll()
                    .requestMatchers("/management/health/liveness").permitAll()
                    .requestMatchers("/management/health/readiness").permitAll()
                    .requestMatchers("/management/prometheus").access(prometheusAccess())
                    .requestMatchers("/management/prometheus/**").access(prometheusAccess())
                    .requestMatchers(HttpMethod.GET, "/management/logs").access(logsAccess(LOGS_GET_PRIVILEGE))
                    .requestMatchers(HttpMethod.PUT, "/management/logs").access(logsAccess(LOGS_UPDATE_PRIVILEGE))
                    .requestMatchers("/management/**").access(managementAccess())
                    .requestMatchers("/v3/api-docs/**").access(apiDocsAccess())
                    .requestMatchers("/v3/api-docs.yaml").access(apiDocsAccess())
                    .requestMatchers("/swagger-ui/**").access(swaggerUiConfigAccess())
                    .requestMatchers("/swagger-ui.html").access(swaggerUiConfigAccess())
                    .requestMatchers("/swagger-resources/configuration/ui").access(swaggerUiConfigAccess())
                    .requestMatchers("/swagger-resources").access(swaggerUiConfigAccess())
                    .anyRequest().access(authorizationManager)
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder())))
            .oauth2Client(oauth2Client -> oauth2Client
                .authorizationCodeGrant(grant -> grant
                    .authorizationRequestResolver(resolver)
                    .authorizationRequestRepository(repository)))
            .authenticationProvider(provider())
            .oauth2Login(oauth2Login -> oauth2Login
                .successHandler(idpSuccessHandler)
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestResolver(resolver)
                    .authorizationRequestRepository(repository)))
            .addFilterBefore(new ReservedPkceParametersFilter(), OAuth2AuthorizationRequestRedirectFilter.class);
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        try {
            RSAPublicKey publicKey = xmConfigServerService.getPublicKeyFromConfigServer();
            return NimbusJwtDecoder.withPublicKey(publicKey).build();
        } catch (Exception e) {
            log.error("Failed to create JWT decoder", e);
            throw new RuntimeException("Failed to create JWT decoder", e);
        }
    }

    /**
     * Used when no bean of type {@code AuthorizationRequestRepository<OAuth2AuthorizationRequest>} is defined:
     * any such bean replaces this default.
     */
    private AuthorizationRequestRepository<OAuth2AuthorizationRequest> defaultAuthorizationRequestRepository() {
        return TRUE.equals(applicationProperties.getDisableIdpCookieUsage()) ?
            new HttpSessionOAuth2AuthorizationRequestRepository() :
            new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    private OidcAuthorizationCodeAuthenticationProvider provider() {
        var tokenResponseClient = new RestClientAuthorizationCodeTokenResponseClient();
        var oidcUserService = new OidcUserService();

        var provider = new OidcAuthorizationCodeAuthenticationProvider(tokenResponseClient, oidcUserService);
        provider.setJwtDecoderFactory(new XmJwtDecoderFactory(tenantContextHolder));
        return provider;
    }

    private AuthorizationManager<RequestAuthorizationContext> managementAccess() {
        return AuthorityAuthorizationManager.hasAuthority(applicationProperties.getManagement().getAccess().getRole());
    }

    private AuthorizationManager<RequestAuthorizationContext> prometheusAccess() {
        return applicationProperties.getPrometheus().isPublic()
            ? SingleResultAuthorizationManager.permitAll()
            : managementAccess();
    }

    private AuthorizationManager<RequestAuthorizationContext> logsAccess(String privilege) {
        return applicationProperties.getManagement().getLogs().isSystemAdminOnly()
            ? managementAccess()
            : anyOf(managementAccess(), privilegeAccess(privilege));
    }

    private AuthorizationManager<RequestAuthorizationContext> routedLogsAccess() {
        return applicationProperties.getManagement().getLogs().isSystemAdminOnly()
            ? managementAccess()
            : AuthenticatedAuthorizationManager.authenticated();
    }

    private AuthorizationManager<RequestAuthorizationContext> apiDocsAccess() {
        return apiDocsPrivilegeCheck
            ? privilegeAccess(API_DOCS_PRIVILEGE)
            : AuthorityAuthorizationManager.hasAnyAuthority(SUPER_ADMIN, ADMIN);
    }

    private AuthorizationManager<RequestAuthorizationContext> swaggerUiConfigAccess() {
        return apiDocsPrivilegeCheck
            ? privilegeAccess(API_DOCS_PRIVILEGE)
            : SingleResultAuthorizationManager.permitAll();
    }

    private AuthorizationManager<RequestAuthorizationContext> privilegeAccess(String privilege) {
        return (authentication, context) -> new AuthorizationDecision(permissionCheckService.hasPermission(privilege));
    }

    private LogoutHandler logoutHandler() {
        return new CookieClearingLogoutHandler(JSESSIONID_COOKIE_NAME);
    }

    private OAuth2AuthorizationRequestResolver requestResolver() {
        return new XmAuthorizationRequestResolver(idpClientRepository, "/oauth2/authorization");
    }

    private static final class ReservedPkceParametersFilter extends OncePerRequestFilter {

        private static final List<String> RESERVED = List.of("code_verifier", "code_challenge", "code_challenge_method");

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return !request.getRequestURI().startsWith("/oauth2/authorization/");
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            for (String name : RESERVED) {
                if (request.getParameter(name) != null) {
                    log.warn("IdP authorization request rejected: reserved parameter {}", name);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
            chain.doFilter(request, response);
        }
    }
}
