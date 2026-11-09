package com.icthh.xm.gate.security.oauth2;

import com.google.protobuf.InvalidProtocolBufferException;
import com.icthh.xm.gate.security.oauth2.dto.OAuth2AuthorizationRequest.OAuth2AuthorizationRequestDto;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

/**
 * Maps an {@link OAuth2AuthorizationRequest} to bytes and back, optionally through a {@link BytesEncryptor}.
 * Any malformed or undecryptable input is reported as one {@link IllegalArgumentException}.
 */
public final class OAuth2AuthorizationRequestCodec {

    private final BytesEncryptor encryptor;

    public OAuth2AuthorizationRequestCodec() {
        this(null);
    }

    public OAuth2AuthorizationRequestCodec(BytesEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    public byte[] encode(OAuth2AuthorizationRequest authorizationRequest) {
        byte[] plain = OAuth2AuthorizationRequestDto.newBuilder()
            .setAuthorizationUri(authorizationRequest.getAuthorizationUri())
            .setClientId(authorizationRequest.getClientId())
            .setRedirectUri(authorizationRequest.getRedirectUri())
            .addAllScopes(authorizationRequest.getScopes())
            .setState(authorizationRequest.getState())
            .putAllAttributes(stringify(authorizationRequest.getAttributes()))
            .putAllAdditionalParameters(stringify(authorizationRequest.getAdditionalParameters()))
            .setAuthorizationRequestUri(authorizationRequest.getAuthorizationRequestUri())
            .build()
            .toByteArray();
        return encryptor == null ? plain : encryptor.encrypt(plain);
    }

    public OAuth2AuthorizationRequest decode(byte[] payload) {
        try {
            byte[] plain = encryptor == null ? payload : encryptor.decrypt(payload);
            OAuth2AuthorizationRequestDto dto = OAuth2AuthorizationRequestDto.parseFrom(plain);
            return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(dto.getAuthorizationUri())
                .clientId(dto.getClientId())
                .redirectUri(dto.getRedirectUri())
                .scopes(new HashSet<>(dto.getScopesList()))
                .state(dto.getState())
                .additionalParameters(new HashMap<>(dto.getAdditionalParametersMap()))
                .attributes(new HashMap<>(dto.getAttributesMap()))
                .authorizationRequestUri(dto.getAuthorizationRequestUri())
                .build();
        } catch (InvalidProtocolBufferException | RuntimeException e) {
            throw new IllegalArgumentException("Malformed authorization request payload");
        }
    }

    private static Map<String, String> stringify(Map<String, Object> input) {
        Map<String, String> result = new HashMap<>();
        Map<String, Object> source = firstNonNull(input, emptyMap());
        source.forEach((key, value) -> result.put(key, String.valueOf(value)));
        return result;
    }
}
