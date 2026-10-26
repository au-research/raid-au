package au.org.raid.orcidupdater.crypto;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Encrypts the email fingerprint into the invitation link, so the updater stores it only if the
 * researcher ticks "Remember me" (Decision, RAID-1002, 2026-10-26). The link's random token is
 * the associated data, so an encrypted fingerprint only opens with the token it was issued with.
 */
@Component
public class LinkCipher {
    static final int MINIMUM_SECRET_LENGTH = AesGcm.MINIMUM_SECRET_LENGTH;

    private final AesGcm aesGcm;

    public LinkCipher(final OrcidUpdaterProperties properties) {
        this.aesGcm = new AesGcm(properties.getLinkEncryptionKey(), "orcid-updater link encryption v1",
                "orcid-updater.link-encryption-key");
    }

    public byte[] encrypt(final byte[] plaintext, final byte[] associatedData) {
        return aesGcm.encrypt(plaintext, associatedData);
    }

    /** Empty when the value was altered, truncated, or issued with a different token. */
    public Optional<byte[]> decrypt(final byte[] sealed, final byte[] associatedData) {
        return aesGcm.decrypt(sealed, associatedData);
    }
}
