package au.org.raid.orcidupdater.crypto;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Encrypts the updater's private signing key at rest (RAID-1005), with its key id as associated
 * data. Keyed from the token encryption key, under its own label.
 */
@Component
public class ServiceKeyCipher {
    private final AesGcm aesGcm;

    public ServiceKeyCipher(final OrcidUpdaterProperties properties) {
        this.aesGcm = new AesGcm(properties.getTokenEncryptionKey(), "orcid-updater service key v1",
                "orcid-updater.token-encryption-key");
    }

    public byte[] encrypt(final byte[] privateKey, final String keyId) {
        return aesGcm.encrypt(privateKey, keyId.getBytes(StandardCharsets.UTF_8));
    }

    /** Empty when the stored key doesn't open, e.g. after the token encryption key was changed. */
    public Optional<byte[]> decrypt(final byte[] sealed, final String keyId) {
        return aesGcm.decrypt(sealed, keyId.getBytes(StandardCharsets.UTF_8));
    }
}
