package com.icthh.xm.gate.security.oauth2;

import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import com.icthh.xm.commons.tenant.internal.DefaultTenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;

@ExtendWith(MockitoExtension.class)
public class XmJwtDecoderFactoryUnitTest {

    private final TenantContextHolder tenantContextHolder = new DefaultTenantContextHolder();

    @BeforeEach
    void setUp() {
        tenantContextHolder.getPrivilegedContext().destroyCurrentContext();
    }

    @Test
    public void test_shouldCreateJwtDecoder() {
        String tenantKey = "tenant1";
        String registrationId = "Auth0";
        String clientName = "Test client";
        String clientId = "f4rzAwx55Dlocg8T4H7ZHYuTgc7pgUej";

        TenantContextUtils.setTenant(tenantContextHolder, tenantKey);
        XmJwtDecoderFactory xmJwtDecoderFactory = new XmJwtDecoderFactory(tenantContextHolder);

        ClientRegistration clientRegistration = createClientRegistration(registrationId, clientName, clientId);

        JwtDecoder createdJwtDecoder = xmJwtDecoderFactory.createDecoder(clientRegistration);

        assertNotNull(createdJwtDecoder);
        Map<String, JwtDecoder> cachedJwtDecoders = XmJwtDecoderFactory.getJwtDecoders(tenantKey);
        assertNotNull(cachedJwtDecoders);
        JwtDecoder cachedJwtDecoder = cachedJwtDecoders.get(clientRegistration);
        assertNotNull(cachedJwtDecoder);
    }


    private ClientRegistration createClientRegistration(String registrationId, String clientName, String clientId) {

        String stubUri = "http://localhost";
        return ClientRegistration.withRegistrationId((registrationId))
            .clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
            .clientName(clientName)
            .clientId(clientId)
            .redirectUri(stubUri)
            .authorizationGrantType(AUTHORIZATION_CODE)
            .authorizationUri(stubUri)
            .tokenUri(stubUri)
            .userInfoUri(stubUri)
            .jwkSetUri(stubUri)
            .clientSecret("secret")
            .build();
    }

}
