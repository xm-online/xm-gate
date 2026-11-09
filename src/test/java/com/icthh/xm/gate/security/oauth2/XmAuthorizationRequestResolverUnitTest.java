package com.icthh.xm.gate.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;

import static org.assertj.core.api.Assertions.assertThat;

class XmAuthorizationRequestResolverUnitTest {

    private final ClientRegistration registration = ClientRegistration.withRegistrationId("idp")
        .clientId("client")
        .clientSecret("secret")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("http://localhost/login/oauth2/code/idp")
        .scope("openid")
        .authorizationUri("http://idp/authorize")
        .tokenUri("http://idp/token")
        .build();

    private final XmAuthorizationRequestResolver resolver = new XmAuthorizationRequestResolver(
        new InMemoryClientRegistrationRepository(registration), "/oauth2/authorization");

    @Test
    void callerParametersDoNotReplaceInternalAttributes() {
        OAuth2AuthorizationRequest request = resolver.resolve(request("nonce", "evil", "registration_id", "evil", "foo", "bar"));

        assertThat(request.<String>getAttribute("registration_id")).isEqualTo("idp");
        assertThat(request.<String>getAttribute("code_verifier")).isNotEqualTo("evil");
        assertThat(request.<String>getAttribute("foo")).isNull();
        assertThat(request.<String>getAttribute("nonce")).isNotEqualTo("evil");
        assertThat(request.<String>getAttribute(PkceParameterNames.CODE_VERIFIER)).isNotBlank();
    }

    @Test
    void callerParametersAreAppendedToRedirectUri() {
        OAuth2AuthorizationRequest request = resolver.resolve(request("foo", "bar"));

        assertThat(request.getRedirectUri()).isEqualTo("http://localhost/login/oauth2/code/idp?foo=bar");
    }

    private static MockHttpServletRequest request(String... params) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/idp");
        for (int i = 0; i < params.length; i += 2) {
            request.setParameter(params[i], params[i + 1]);
        }
        return request;
    }
}
