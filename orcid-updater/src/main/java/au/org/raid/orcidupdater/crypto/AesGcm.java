package au.org.raid.orcidupdater.crypto;

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
 * AES-256-GCM with a random nonce, keyed from a configured secret. Each use passes its own
 * label, so one secret reused for two settings still gives two different keys.
 */
final class AesGcm {
    static final int MINIMUM_SECRET_LENGTH = 32;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    /** @param settingName named in the startup failure, which never contains the secret itself */
    AesGcm(final String secret, final String label, final String settingName) {
        if (secret.length() < MINIMUM_SECRET_LENGTH) {
            throw new IllegalStateException("%s must be at least %d characters"
                    .formatted(settingName, MINIMUM_SECRET_LENGTH));
        }
        this.key = new SecretKeySpec(deriveKey(secret, label), "AES");
    }

    private static byte[] deriveKey(final String secret, final String label) {
        try {
            final var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(label.getBytes(StandardCharsets.UTF_8));
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA-256 unavailable", e);
        }
    }

    byte[] encrypt(final byte[] plaintext, final byte[] associatedData) {
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

    /** Empty when the value was altered, truncated, or sealed with different associated data. */
    Optional<byte[]> decrypt(final byte[] sealed, final byte[] associatedData) {
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
