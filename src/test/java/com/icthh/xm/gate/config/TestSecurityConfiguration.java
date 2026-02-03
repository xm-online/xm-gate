package com.icthh.xm.gate.config;

import static com.icthh.xm.commons.config.client.config.XmRestTemplateConfiguration.XM_CONFIG_REST_TEMPLATE;
import static org.mockito.Mockito.mock;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.security.jwt.TokenProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * This class allows you to run unit and integration tests without an IdP.
 */
@TestConfiguration
public class TestSecurityConfiguration {

    @Bean
    @Primary
    public TokenProvider tokenProvider() {
        return mock(TokenProvider.class);
    }

    @Bean
    public CommonConfigRepository commonConfigRepository() {
        return mock(CommonConfigRepository.class);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return mock(JwtDecoder.class);
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    @Primary
    public RestClient loadBalancedRestTemplate() {
        return mock(RestClient.class);
    }

    @Bean(XM_CONFIG_REST_TEMPLATE)
    public RestTemplate restTemplate() {
        return mock(RestTemplate.class);
    }
}
