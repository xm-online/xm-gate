package com.icthh.xm.gate.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2AuthorizationRequestCodecUnitTest {

    private static final BytesEncryptor REVERSING = new BytesEncryptor() {
        @Override
        public byte[] encrypt(byte[] bytes) {
            return reverse(bytes);
        }

        @Override
        public byte[] decrypt(byte[] bytes) {
            return reverse(bytes);
        }
    };

    @Test
    void roundTrip() {
        OAuth2AuthorizationRequestCodec codec = new OAuth2AuthorizationRequestCodec();

        OAuth2AuthorizationRequest decoded = codec.decode(codec.encode(request()));

        assertThat(decoded.getState()).isEqualTo("state");
        assertThat(decoded.getClientId()).isEqualTo("client");
        assertThat(decoded.getScopes()).containsExactly("scope");
        assertThat(decoded.<String>getAttribute("a")).isEqualTo("b");
        assertThat(decoded.getAdditionalParameters()).containsEntry("c", "d");
    }

    @Test
    void garbageIsRejected() {
        OAuth2AuthorizationRequestCodec codec = new OAuth2AuthorizationRequestCodec();

        assertThatThrownBy(() -> codec.decode(new byte[]{1, 2, 3}))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void storedBytesAreTheEncryptorOutput() {
        OAuth2AuthorizationRequest request = request();
        byte[] plain = new OAuth2AuthorizationRequestCodec().encode(request);
        OAuth2AuthorizationRequestCodec codec = new OAuth2AuthorizationRequestCodec(REVERSING);

        byte[] stored = codec.encode(request);

        assertThat(stored).isEqualTo(reverse(plain));
        assertThat(codec.decode(stored).getState()).isEqualTo("state");
    }

    private static byte[] reverse(byte[] bytes) {
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[bytes.length - 1 - i];
        }
        return result;
    }

    private static OAuth2AuthorizationRequest request() {
        return OAuth2AuthorizationRequest.authorizationCode()
            .authorizationUri("uri")
            .clientId("client")
            .redirectUri("redirect")
            .attributes(Map.of("a", "b"))
            .authorizationRequestUri("auth")
            .additionalParameters(Map.of("c", "d"))
            .scopes(Set.of("scope"))
            .state("state")
            .build();
    }
}
