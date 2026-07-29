package io.nimbus.platform.github.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * MVP 로컬용 토큰 암호화 (AES).
 * v2: KMS / Vault SecretProvider 로 교체.
 */
@Service
public class TokenCryptoService {

    private final SecretKeySpec keySpec;

    public TokenCryptoService(
            @Value("${nimbus.auth.jwt-secret:nimbus-local-dev-secret-change-me-32bytes-min}") String secret
    ) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            this.keySpec = new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to init token crypto", e);
        }
    }

    public String encrypt(String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Token encrypt failed", e);
        }
    }

    public String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Token decrypt failed", e);
        }
    }
}
