package io.nimbus.platform.github.crypto;

import java.util.Base64;

/**
 * Thin sealed-box for GitHub Actions secrets.
 * <p>
 * Full libsodium crypto_box_seal is not bundled (free-only / zero native dep).
 * {@link #trySeal} returns empty when encryption is unavailable — callers use SIMULATED mode.
 * Live encrypt can be plugged in later (libsodium / tweetnacl).
 */
public final class GitHubActionsSecretCipher {

    private GitHubActionsSecretCipher() {
    }

    /**
     * @return base64 sealed value, or empty if not supported in this build
     */
    public static java.util.Optional<String> trySeal(String plaintext, String publicKeyBase64) {
        if (plaintext == null || publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            // Validate key decodes — real seal deferred without native sodium
            Base64.getDecoder().decode(publicKeyBase64);
            return java.util.Optional.empty();
        } catch (Exception ex) {
            return java.util.Optional.empty();
        }
    }
}
