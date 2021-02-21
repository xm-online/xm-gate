package com.icthh.xm.gate.security.oauth2;

import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.IdpPublicClientConfig;
import com.icthh.xm.commons.domain.idp.model.IdpPublicConfig.IdpConfigContainer.IdpAccessTokenInclusion;
import com.icthh.xm.commons.domain.idp.model.IdpPrivateConfig.IdpConfigContainer.IdpPrivateClientConfig;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class IdpTestUtils {

    public static IdpPublicClientConfig buildIdpPublicClientConfig(String key, String clientId) {
        IdpPublicClientConfig idpPublicClientConfig = new IdpPublicClientConfig();
        IdpPublicClientConfig.OpenIdConfig openIdConfig = new IdpPublicClientConfig.OpenIdConfig();

        idpPublicClientConfig.setKey(key);
        idpPublicClientConfig.setClientId(clientId);
        idpPublicClientConfig.setName(key);
        idpPublicClientConfig.setRedirectUri("http://localhost:4200");

        openIdConfig.setAuthorizationEndpoint(buildAuthorizationEndpoint());
        openIdConfig.setTokenEndpoint(buildTokenEndpoint());
        openIdConfig.setUserinfoEndpoint(buildUserInfoEndpoint());
        openIdConfig.setEndSessionEndpoint(buildEndSessionEndpoint());
        openIdConfig.setJwksEndpoint(buildJwksEndpoint());

        idpPublicClientConfig.setOpenIdConfig(openIdConfig);

        return idpPublicClientConfig;
    }

    private static IdpPublicClientConfig.OpenIdConfig.UserInfoEndpoint buildUserInfoEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.UserInfoEndpoint userinfoEndpoint = new IdpPublicClientConfig.OpenIdConfig.UserInfoEndpoint();
        userinfoEndpoint.setUri("https://idp1.com/userinfo");
        userinfoEndpoint.setUserNameAttributeName("email");
        return userinfoEndpoint;
    }

    private static IdpPublicClientConfig.OpenIdConfig.BaseEndpoint buildEndSessionEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.BaseEndpoint userinfoEndpoint = new IdpPublicClientConfig.OpenIdConfig.BaseEndpoint();
        userinfoEndpoint.setUri("https://idp1.com/logout");
        return userinfoEndpoint;
    }

    private static IdpPublicClientConfig.OpenIdConfig.BaseEndpoint buildJwksEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.BaseEndpoint userinfoEndpoint = new IdpPublicClientConfig.OpenIdConfig.BaseEndpoint();
        userinfoEndpoint.setUri("https://idp1.com/.well-known/jwks.json");
        return userinfoEndpoint;
    }

    private static IdpPublicClientConfig.OpenIdConfig.TokenEndpoint buildTokenEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.TokenEndpoint tokenEndpoint = new IdpPublicClientConfig.OpenIdConfig.TokenEndpoint();
        tokenEndpoint.setUri("https://idp1.com/oauth/token");
        tokenEndpoint.setGrantType("authorization_code");
        return tokenEndpoint;
    }

    private static IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint buildAuthorizationEndpoint() {
        IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint authorizationEndpoint = new IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint();

        authorizationEndpoint.setUri("https://idp1.com/authorize");
        authorizationEndpoint.setResponseType("code");
        authorizationEndpoint.setAdditionalParams(Map.of("connection", "google-oauth2"));

        IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint.Features features = new IdpPublicClientConfig.OpenIdConfig.AuthorizationEndpoint.Features();
        features.setState(true);
        authorizationEndpoint.setFeatures(features);

        return authorizationEndpoint;
    }

    public static IdpAccessTokenInclusion buildFeatures() {
        IdpAccessTokenInclusion features = new IdpAccessTokenInclusion();

        features.setPkce(false);
        features.setStateful(false);

        IdpAccessTokenInclusion.Bearirng idpAccessTokenInclusion = new IdpAccessTokenInclusion.Bearirng();

        idpAccessTokenInclusion.setEnabled(true);
        idpAccessTokenInclusion.setIdpTokenHeader("Authorization");
        idpAccessTokenInclusion.setXmTokenHeader("X-Authorization");

        features.setBearirng(idpAccessTokenInclusion);

        return features;
    }


    public static IdpPrivateConfig buildPrivateConfig(String key, int clientsAmount, boolean buildValidConfig) {
        if (!buildValidConfig) {
            return new IdpPrivateConfig();
        }
        IdpPrivateConfig idpPrivateConfig = new IdpPrivateConfig();
        IdpPrivateConfig.IdpConfigContainer config = new IdpPrivateConfig.IdpConfigContainer();

        List<IdpPrivateClientConfig> idpPrivateClientConfigs = new ArrayList<>();
        for (int i = 0; i < clientsAmount; i++) {
            idpPrivateClientConfigs.add(buildIdpPrivateClientConfigs(key + i));
        }

        config.setClients(idpPrivateClientConfigs);

        idpPrivateConfig.setConfig(config);

        return idpPrivateConfig;
    }

    private static IdpPrivateClientConfig buildIdpPrivateClientConfigs(String key) {
        IdpPrivateClientConfig idpPrivateClientConfig = new IdpPrivateClientConfig();

        idpPrivateClientConfig.setKey(key);
        idpPrivateClientConfig.setClientSecret("client-secret");
        idpPrivateClientConfig.setScope(Set.of("openid", "profile", "email"));

        return idpPrivateClientConfig;
    }

    public static Authentication buildAuthentication(String userNameAttributeName) {
        userNameAttributeName = "email";
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ADMIN"));

        OidcIdToken oidcIdToken = buildOidcToken(
            getIdToken(),
            Map.of(
                "aud", "VtRxEs7qN4oSte7Jap7gXn83mfK1Ww20",
                "iss", "https://test.com/",
                "email", "test@test.com"
            ));
        OidcUser principal = getUser(userNameAttributeName, oidcIdToken, null, authorities);

        return new OAuth2AuthenticationToken(principal, authorities, "client_registration_id");
    }

    private String getIdToken() {
        return "idp.token.value";
    }

    private OidcUser getUser(String userNameAttributeName, OidcIdToken idToken, OidcUserInfo userInfo, Set<GrantedAuthority> authorities) {
        if (StringUtils.hasText(userNameAttributeName)) {
            return new DefaultOidcUser(authorities, idToken, userNameAttributeName);
        }
        return new DefaultOidcUser(authorities, idToken, userInfo);
    }

    private OidcIdToken buildOidcToken(String token, Map<String, Object> claims) {
        Instant issuedAt = Instant.ofEpochSecond(1610450173);
        Instant expiresAt = Instant.ofEpochSecond(1610486173);

        return new OidcIdToken(token, issuedAt, expiresAt, claims);
    }

    public static IdpPublicConfig buildPublicConfig(String key, int clientsAmount, String clientId, boolean isValidConfig) {
        if (!isValidConfig) {
            return new IdpPublicConfig();
        }

        IdpPublicConfig idpPublicConfig = new IdpPublicConfig();
        IdpPublicConfig.IdpConfigContainer config = new IdpPublicConfig.IdpConfigContainer();

        config.setDirectLogin(true);
        List<IdpPublicClientConfig> idpPublicClientConfigs = new ArrayList<>();
        for (int i = 0; i < clientsAmount; i++) {
            idpPublicClientConfigs.add(buildIdpPublicClientConfig(key + i, clientId + i));
        }
        config.setClients(idpPublicClientConfigs);
        config.setIdpAccessTokenInclusion(buildFeatures());

        idpPublicConfig.setConfig(config);

        return idpPublicConfig;
    }

}
