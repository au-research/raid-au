package au.org.raid.orcidupdater.crypto;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Locale;

/**
 * One-way fingerprint of a researcher's email: HMAC-SHA-256 keyed with the RA's secret, so the
 * fingerprint can't be reversed by hashing guessed addresses without that secret (RAID-975).
 * The email is trimmed and lowercased first, so " Researcher@Example.edu " and
 * "researcher@example.edu" match.
 */
@Component
public class EmailHasher {
    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public EmailHasher(final OrcidUpdaterProperties properties) {
        this.key = new SecretKeySpec(properties.getEmailHashSecret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public static String normalise(final String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public byte[] hash(final String email) {
        try {
            final var mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac.doFinal(normalise(email).getBytes(StandardCharsets.UTF_8));
        } catch (final GeneralSecurityException e) {
            // HmacSHA256 is mandatory in every Java runtime, so this means a broken JVM
            throw new IllegalStateException("HMAC-SHA-256 unavailable", e);
        }
    }
}
