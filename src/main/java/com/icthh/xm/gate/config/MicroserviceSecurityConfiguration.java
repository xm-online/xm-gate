package com.icthh.xm.gate.config;

import com.icthh.xm.commons.security.RoleConstant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.gate.config.properties.ApplicationProperties;
import com.icthh.xm.gate.gateway.accesscontrol.AccessControlAuthorizationManager;
import com.icthh.xm.gate.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.icthh.xm.gate.security.oauth2.idp.IdpAuthenticationSuccessHandler;
import com.icthh.xm.gate.security.oauth2.idp.IdpClientRepository;
import com.icthh.xm.gate.security.oauth2.XmAuthorizationRequestResolver;
import com.icthh.xm.gate.security.oauth2.XmConfigServerService;
import com.icthh.xm.gate.security.oauth2.XmJwtDecoderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import java.security.interfaces.RSAPublicKey;

import static com.icthh.xm.gate.config.Constants.JSESSIONID_COOKIE_NAME;
import static java.lang.Boolean.TRUE;

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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
                    .requestMatchers("/*/api/public/**").permitAll()
                    .requestMatchers("/oauth2/authorization/**").permitAll()
                    .requestMatchers("/login/oauth2/code/**").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .requestMatchers("/*/public/**").permitAll()
                    .requestMatchers("/*/oauth/**").permitAll()
                    .requestMatchers("/management/health").permitAll()
                    .requestMatchers("/management/prometheus/**").permitAll()
                    .requestMatchers("/management/**").hasAuthority(RoleConstant.SUPER_ADMIN)
                    .anyRequest().access(authorizationManager)
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder())))
            .oauth2Client(oauth2Client -> oauth2Client
                .authorizationCodeGrant(grant -> grant
                    .authorizationRequestResolver(requestResolver())
                    .authorizationRequestRepository(authorizationRequestRepository())))
            .authenticationProvider(provider())
            .oauth2Login(oauth2Login -> oauth2Login
                .successHandler(idpSuccessHandler)
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestRepository(authorizationRequestRepository())));
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

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
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

    private LogoutHandler logoutHandler() {
        return new CookieClearingLogoutHandler(JSESSIONID_COOKIE_NAME);
    }

    private OAuth2AuthorizationRequestResolver requestResolver() {
        return new XmAuthorizationRequestResolver(idpClientRepository, "/oauth2/authorization");
    }
}
