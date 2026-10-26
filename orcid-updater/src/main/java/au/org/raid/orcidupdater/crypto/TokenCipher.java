package au.org.raid.orcidupdater.crypto;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Encrypts researchers' ORCID access and refresh tokens at rest (RAID-975). The researcher's iD
 * is the associated data, so a token copied onto another researcher's row won't decrypt.
 */
@Component
public class TokenCipher {
    private final AesGcm aesGcm;

    public TokenCipher(final OrcidUpdaterProperties properties) {
        this.aesGcm = new AesGcm(properties.getTokenEncryptionKey(), "orcid-updater token encryption v1",
                "orcid-updater.token-encryption-key");
    }

    public byte[] encrypt(final String token, final String orcidId) {
        return aesGcm.encrypt(token.getBytes(StandardCharsets.UTF_8), orcidId.getBytes(StandardCharsets.UTF_8));
    }

    /** @throws IllegalStateException if the stored value doesn't open with this key and iD */
    public String decrypt(final byte[] sealed, final String orcidId) {
        return aesGcm.decrypt(sealed, orcidId.getBytes(StandardCharsets.UTF_8))
                .map(plaintext -> new String(plaintext, StandardCharsets.UTF_8))
                .orElseThrow(() -> new IllegalStateException("Stored ORCID token for " + orcidId
                        + " can't be decrypted with orcid-updater.token-encryption-key"));
    }
}
