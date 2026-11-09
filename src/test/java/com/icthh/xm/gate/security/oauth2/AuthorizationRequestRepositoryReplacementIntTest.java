package com.icthh.xm.gate.security.oauth2;

import com.icthh.xm.gate.IntegrationTest;
import com.icthh.xm.gate.security.oauth2.idp.IdpClientRepository;
import com.icthh.xm.gate.service.TenantMappingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@IntegrationTest
@AutoConfigureMockMvc
@Import(AuthorizationRequestRepositoryReplacementIntTest.RecordingConfig.class)
class AuthorizationRequestRepositoryReplacementIntTest {

    static final List<String> CALLS = new CopyOnWriteArrayList<>();

    // not a @TestConfiguration: that would be component-scanned into every other test context
    static class RecordingConfig {
        @Bean
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> recordingRepository() {
            return new AuthorizationRequestRepository<>() {
                @Override
                public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
                    CALLS.add("load");
                    return null;
                }

                @Override
                public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                                     HttpServletRequest request, HttpServletResponse response) {
                    CALLS.add("save");
                }

                @Override
                public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                             HttpServletResponse response) {
                    CALLS.add("remove");
                    return null;
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private IdpClientRepository idpClientRepository;
    @Autowired
    private TenantMappingService tenantMappingService;

    @Test
    void replacementIsUsedByRedirectAndCallback() throws Exception {
        ClientRegistration registration = ClientRegistration.withRegistrationId("idp")
            .clientId("client")
            .clientSecret("secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/login/oauth2/code/idp")
            .scope("openid")
            .authorizationUri("http://idp.test/authorize")
            .tokenUri("http://idp.test/token")
            .build();
        idpClientRepository.setRegistrations(tenantMappingService.getTenantKey("localhost"), List.of(registration));
        CALLS.clear();

        MockHttpServletResponse redirect = mockMvc.perform(get("/oauth2/authorization/idp")).andReturn().getResponse();
        assertThat(redirect.getStatus()).isEqualTo(302);
        assertThat(redirect.getHeaders("Set-Cookie")).isEmpty();
        assertThat(CALLS).containsExactly("save");

        mockMvc.perform(get("/login/oauth2/code/idp").param("code", "x").param("state", "s"));
        assertThat(CALLS).contains("remove");
    }
}
