package au.org.raid.orcidupdater.crypto;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import org.springframework.stereotype.Component;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Optional;

/**
 * Encrypts the email fingerprint into the invitation link, so the updater stores it only if the
 * researcher ticks "Remember me" (Decision, RAID-1002, 2026-10-26). AES-256-GCM with a random
 * nonce. The link's random token is the associated data, so an encrypted fingerprint only opens
 * with the token it was issued with.
 */
@Component
public class LinkCipher {
    static final int MINIMUM_SECRET_LENGTH = 32;
    private static final String KEY_DERIVATION_LABEL = "orcid-updater link encryption v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public LinkCipher(final OrcidUpdaterProperties properties) {
        final var secret = properties.getLinkEncryptionKey();
        // Reported by length only: the failure message must never contain the secret
        if (secret.length() < MINIMUM_SECRET_LENGTH) {
            throw new IllegalStateException("orcid-updater.link-encryption-key must be at least %d characters"
                    .formatted(MINIMUM_SECRET_LENGTH));
        }
        this.key = new SecretKeySpec(deriveKey(secret), "AES");
    }

    /** An AES key from the configured secret, separate from any other key the secret might feed. */
    private static byte[] deriveKey(final String secret) {
        try {
            final var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(KEY_DERIVATION_LABEL.getBytes(StandardCharsets.UTF_8));
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA-256 unavailable", e);
        }
    }

    public byte[] encrypt(final byte[] plaintext, final byte[] associatedData) {
        try {
            final var nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            final var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(associatedData);
            final var ciphertext = cipher.doFinal(plaintext);
            return ByteBuffer.allocate(NONCE_BYTES + ciphertext.length).put(nonce).put(ciphertext).array();
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM encryption failed", e);
        }
    }

    /** Empty when the value was altered, truncated, or issued with a different token. */
    public Optional<byte[]> decrypt(final byte[] sealed, final byte[] associatedData) {
        if (sealed.length <= NONCE_BYTES) {
            return Optional.empty();
        }
        try {
            final var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, sealed, 0, NONCE_BYTES));
            cipher.updateAAD(associatedData);
            return Optional.of(cipher.doFinal(sealed, NONCE_BYTES, sealed.length - NONCE_BYTES));
        } catch (final AEADBadTagException e) {
            return Optional.empty();
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM decryption failed", e);
        }
    }
}
