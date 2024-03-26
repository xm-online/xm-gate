package com.icthh.xm.gate.config;

import com.icthh.xm.commons.permission.constants.RoleConstant;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.gate.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.icthh.xm.gate.security.oauth2.IdpAuthenticationSuccessHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import com.icthh.xm.gate.security.oauth2.XmJwtDecoderFactory;
import com.icthh.xm.gate.security.session.CustomSessionFlashMapManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.SessionFlashMapManager;

import static com.icthh.xm.gate.config.Constants.JSESSIONID_COOKIE_NAME;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class MicroserviceSecurityConfiguration extends ResourceServerConfigurerAdapter {

    private final DiscoveryClient discoveryClient;

    private final TenantContextHolder tenantContextHolder;

    private final IdpAuthenticationSuccessHandler idpSuccessHandler;

    private final RestTemplateErrorHandler restTemplateErrorHandler;

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
            .csrf()
            .disable()
            .headers()
            .frameOptions()
            .disable()
        .and().logout()
            .logoutUrl("/")
            .addLogoutHandler(logoutHandler())
            .permitAll()
        .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
            .authorizeRequests()
            //convention: allow to process /api/public for all service
            .antMatchers("/*/api/public/**").permitAll()
            .antMatchers("/api/profile-info").permitAll()
            .antMatchers("/oauth2/authorization/**").permitAll()
            .antMatchers("/login/oauth2/code/**").permitAll()
            .antMatchers("/api/**").authenticated()
            .antMatchers("/management/health").permitAll()
            .antMatchers("/management/prometheus/**").permitAll()
            .antMatchers("/management/**").hasAuthority(RoleConstant.SUPER_ADMIN)
            .antMatchers("/swagger-resources/configuration/ui").permitAll()
        .and()
            .oauth2Client()
            .authorizationCodeGrant()
            .authorizationRequestRepository(authorizationRequestRepository())
            .and() // second and to go to upper level from grant configurer back to oauthClient
        .and().authenticationProvider(provider())
            .oauth2Login()
            .successHandler(idpSuccessHandler)
            .authorizationEndpoint()
            .authorizationRequestRepository(authorizationRequestRepository())
        ;
    }

    @Bean
    public TokenStore tokenStore(JwtAccessTokenConverter jwtAccessTokenConverter) {
        return new JwtTokenStore(jwtAccessTokenConverter);
    }

    public OidcAuthorizationCodeAuthenticationProvider provider() {
        DefaultAuthorizationCodeTokenResponseClient defaultAuthorizationCodeTokenResponseClient =
            new DefaultAuthorizationCodeTokenResponseClient();

        OidcUserService oidcUserService = new OidcUserService();

        OidcAuthorizationCodeAuthenticationProvider oidcAuthorizationCodeAuthenticationProvider =
            new OidcAuthorizationCodeAuthenticationProvider(defaultAuthorizationCodeTokenResponseClient, oidcUserService);

        oidcAuthorizationCodeAuthenticationProvider.setJwtDecoderFactory(new XmJwtDecoderFactory(tenantContextHolder));

        return oidcAuthorizationCodeAuthenticationProvider;
    }

    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter(
        @Qualifier("loadBalancedRestTemplate") RestTemplate keyUriRestTemplate)
        throws CertificateException, IOException {

        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setVerifierKey(getKeyFromConfigServer(keyUriRestTemplate));
        return converter;
    }

    @Bean
    public RestTemplate loadBalancedRestTemplate(RestTemplateCustomizer customizer) {
        RestTemplate restTemplate = new RestTemplate();
        customizer.customize(restTemplate);
        return restTemplate;
    }

    @Bean
    public RestTemplate notBufferRestTemplate() {
        RestTemplate restTemplate = new RestTemplate() {
            @Override
            public void setInterceptors(List<ClientHttpRequestInterceptor> interceptors) {
                log.warn("Interceptors are not supported to by notBufferRestTemplate");
            }
        };
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setBufferRequestBody(false);
        restTemplate.setRequestFactory(requestFactory);
        restTemplate.setErrorHandler(restTemplateErrorHandler);
        return restTemplate;
    }

    private String getKeyFromConfigServer(RestTemplate keyUriRestTemplate) throws CertificateException, IOException {
        // Load available UAA servers
        discoveryClient.getServices();
        HttpEntity<Void> request = new HttpEntity<Void>(new HttpHeaders());
        String content = keyUriRestTemplate
            .exchange("http://config/api/token_key", HttpMethod.GET, request, String.class).getBody();

        if (StringUtils.isBlank(content)) {
            throw new CertificateException("Received empty certificate from config.");
        }

        try (InputStream fin = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {

            CertificateFactory f = CertificateFactory.getInstance(Constants.CERTIFICATE);
            X509Certificate certificate = (X509Certificate) f.generateCertificate(fin);
            PublicKey pk = certificate.getPublicKey();
            return String.format(Constants.PUBLIC_KEY, new String(Base64.getEncoder().encode(pk.getEncoded())));
        }
    }

    @Bean
    public SessionFlashMapManager flashMapManager(){
        return new CustomSessionFlashMapManager();
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    public LogoutHandler logoutHandler(){
        return new CookieClearingLogoutHandler(JSESSIONID_COOKIE_NAME);
    }

}
