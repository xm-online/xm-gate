package com.icthh.xm.gate.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.security.jwt.TokenProvider;
import com.icthh.xm.gate.security.AuthoritiesConstants;
import com.icthh.xm.gate.security.SecurityUtils;
import com.icthh.xm.gate.security.oauth2.AudienceValidator;
import com.icthh.xm.gate.security.oauth2.JwtGrantedAuthorityConverter;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import com.icthh.xm.gate.web.filter.ReactiveJwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.web.filter.reactive.CookieCsrfFilter;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfiguration {

    private final JHipsterProperties jHipsterProperties;

    private final TokenProvider tokenProvider;

    @Value("${application.security.oauth2.enabled}")
    private boolean oauth2Enabled;

    private final ReactiveClientRegistrationRepository clientRegistrationRepository;
    private final ReactiveAuthorizationManager<AuthorizationContext> reactiveAuthorizationManager;

    // See https://github.com/jhipster/generator-jhipster/issues/18868
    // We don't use a distributed cache or the user selected cache implementation here on purpose
    private final Cache<String, Mono<Jwt>> users = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofHours(1))
        .recordStats()
        .build();

    public SecurityConfiguration(ReactiveClientRegistrationRepository clientRegistrationRepository,
                                 JHipsterProperties jHipsterProperties, @Lazy TokenProvider tokenProvider,
                                 ReactiveAuthorizationManager<AuthorizationContext> reactiveAuthorizationManager) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.jHipsterProperties = jHipsterProperties;
        this.tokenProvider = tokenProvider;
        this.reactiveAuthorizationManager = reactiveAuthorizationManager;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .securityMatcher(
                new NegatedServerWebExchangeMatcher(
                    new OrServerWebExchangeMatcher(pathMatchers("/app/**", "/i18n/**", "/content/**", "/swagger-ui/**"))
                )
            )
            .cors(withDefaults())
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            // See https://github.com/spring-projects/spring-security/issues/5766
            .addFilterAt(new CookieCsrfFilter(), SecurityWebFiltersOrder.REACTOR_CONTEXT)
            .headers(
                headers ->
                    headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(jHipsterProperties.getSecurity().getContentSecurityPolicy()))
                        .frameOptions(frameOptions -> frameOptions.mode(Mode.DENY))
                        .referrerPolicy(
                            referrer ->
                                referrer.policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                        .permissionsPolicy(
                            permissions ->
                                permissions.policy(
                                    "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"
                                )
                        )
            )
            .authorizeExchange(
                authz ->
                    // prettier-ignore
                    authz
                        .pathMatchers("/*/api/public/**").permitAll()
                        .pathMatchers("/api/profile-info").permitAll()
                        .pathMatchers("/oauth2/authorization/**").permitAll()
                        .pathMatchers("/login/oauth2/code/**").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .pathMatchers("/v3/api-docs/**").hasAuthority(AuthoritiesConstants.ADMIN)
                        .pathMatchers("/management/health").permitAll()
                        .pathMatchers("/management/prometheus/**").permitAll()
                        .pathMatchers("/management/**").hasAuthority(RoleConstant.SUPER_ADMIN)
                        .pathMatchers("/swagger-resources/**").hasAuthority(RoleConstant.SUPER_ADMIN)
                        .anyExchange().access(reactiveAuthorizationManager)
            )
            .addFilterAfter(new ReactiveJwtFilter(tokenProvider), SecurityWebFiltersOrder.REACTOR_CONTEXT);
        if (oauth2Enabled) {
            http.oauth2Login(oauth2 -> oauth2.authorizationRequestResolver(authorizationRequestResolver(this.clientRegistrationRepository)))
                .oauth2Client(withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                        .jwtDecoder(jwtDecoder(this.clientRegistrationRepository))
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                    )
                );
        }
        return http.build();
    }

    private ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(
        ReactiveClientRegistrationRepository clientRegistrationRepository
    ) {
        DefaultServerOAuth2AuthorizationRequestResolver authorizationRequestResolver = new DefaultServerOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository
        );
        return authorizationRequestResolver;
    }

    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer() {
        return customizer ->
            customizer.authorizationRequestUri(
                uriBuilder -> uriBuilder.queryParam("audience", jHipsterProperties.getSecurity().getOauth2().getAudience()).build()
            );
    }

    Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new JwtGrantedAuthorityConverter());
        jwtAuthenticationConverter.setPrincipalClaimName(PREFERRED_USERNAME);
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

    /**
     * Map authorities from "groups" or "roles" claim in ID Token.
     *
     * @return a {@link ReactiveOAuth2UserService} that has the groups from the IdP.
     */
    @Bean
    public ReactiveOAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcReactiveOAuth2UserService delegate = new OidcReactiveOAuth2UserService();

        return userRequest -> {
            // Delegate to the default implementation for loading a user
            return delegate
                .loadUser(userRequest)
                .map(user -> {
                    Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

                    user
                        .getAuthorities()
                        .forEach(authority -> {
                            if (authority instanceof OidcUserAuthority) {
                                OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) authority;
                                mappedAuthorities.addAll(
                                    SecurityUtils.extractAuthorityFromClaims(oidcUserAuthority.getUserInfo().getClaims())
                                );
                            }
                        });

                    return new DefaultOidcUser(mappedAuthorities, user.getIdToken(), user.getUserInfo(), PREFERRED_USERNAME);
                });
        };
    }

    ReactiveJwtDecoder jwtDecoder(ReactiveClientRegistrationRepository registrations) {
        Mono<ClientRegistration> clientRegistration = registrations.findByRegistrationId("oidc");

        return clientRegistration
            .map(
                oidc ->
                    createJwtDecoder(
                        oidc.getProviderDetails().getIssuerUri(),
                        oidc.getProviderDetails().getJwkSetUri(),
                        oidc.getProviderDetails().getUserInfoEndpoint().getUri()
                    )
            )
            .block();
    }

    private ReactiveJwtDecoder createJwtDecoder(String issuerUri, String jwkSetUri, String userInfoUri) {
        NimbusReactiveJwtDecoder jwtDecoder = new NimbusReactiveJwtDecoder(jwkSetUri);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(jHipsterProperties.getSecurity().getOauth2().getAudience());
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return new ReactiveJwtDecoder() {
            @Override
            public Mono<Jwt> decode(String token) throws JwtException {
                return jwtDecoder.decode(token).flatMap(jwt -> enrich(token, jwt));
            }

            private Mono<Jwt> enrich(String token, Jwt jwt) {
                // Only look up user information if identity claims are missing
                if (jwt.hasClaim("given_name") && jwt.hasClaim("family_name")) {
                    return Mono.just(jwt);
                }
                // Get user info from `users` cache if present
                return Optional.ofNullable(
                        users.getIfPresent(jwt.getSubject())
                    )// Retrieve user info from OAuth provider if not already loaded
                    .orElseGet(() ->
                        WebClient.create()
                            .get()
                            .uri(userInfoUri)
                            .headers(headers -> headers.setBearerAuth(token))
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                            })
                            .map(userInfo ->
                                Jwt.withTokenValue(jwt.getTokenValue())
                                    .subject(jwt.getSubject())
                                    .audience(jwt.getAudience())
                                    .headers(headers -> headers.putAll(jwt.getHeaders()))
                                    .claims(claims -> {
                                        String username = userInfo.get("preferred_username").toString();
                                        // special handling for Auth0
                                        if (userInfo.get("sub").toString().contains("|") && username.contains("@")) {
                                            userInfo.put("email", username);
                                        }
                                        // Allow full name in a name claim - happens with Auth0
                                        if (userInfo.get("name") != null) {
                                            String[] name = userInfo.get("name").toString().split("\\s+");
                                            if (name.length > 0) {
                                                userInfo.put("given_name", name[0]);
                                                userInfo.put("family_name", String.join(" ", Arrays.copyOfRange(name, 1, name.length)));
                                            }
                                        }
                                        claims.putAll(userInfo);
                                    })
                                    .claims(claims -> claims.putAll(jwt.getClaims()))
                                    .build())
                            // Put user info into the `users` cache
                            .doOnNext(newJwt -> users.put(jwt.getSubject(), Mono.just(newJwt))));
            }
        };
    }
}
