package io.nimbus.platform.github;

import io.nimbus.platform.github.crypto.TokenCryptoService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenCryptoServiceTest {

    @Test
    void encryptDecryptRoundTrip() {
        TokenCryptoService crypto = new TokenCryptoService("test-secret-key-for-nimbus-platform-jwt-32b");
        String plain = "ghp_example_token_12345";
        String enc = crypto.encrypt(plain);
        assertThat(enc).isNotEqualTo(plain);
        assertThat(crypto.decrypt(enc)).isEqualTo(plain);
    }
}
