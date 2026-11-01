package au.org.raid.orcidupdater.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * The token in an invitation link: {@code <random>.<sealed email fingerprint>}, both base64url.
 * <ul>
 *   <li>The random part is 256 bits from SecureRandom. The database keeps only its SHA-256
 *       ({@link #lookupHash()}), so a copy of the database yields no working links.</li>
 *   <li>The sealed part is the email fingerprint encrypted by {@link LinkCipher} with the random
 *       part as associated data. It is opened only if the researcher ticks "Remember me".</li>
 * </ul>
 */
public record InvitationLinkToken(byte[] random, byte[] sealedEmailHash) {
    private static final int RANDOM_BYTES = 32;
    private static final char SEPARATOR = '.';
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    public static InvitationLinkToken issue(final byte[] emailHash, final LinkCipher cipher) {
        final var random = new byte[RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(random);
        return new InvitationLinkToken(random, cipher.encrypt(emailHash, random));
    }

    /** Empty for anything that isn't a well-formed token, so callers treat it as an unknown link. */
    public static Optional<InvitationLinkToken> parse(final String value) {
        final var separator = value.indexOf(SEPARATOR);
        if (separator <= 0 || separator != value.lastIndexOf(SEPARATOR) || separator == value.length() - 1) {
            return Optional.empty();
        }
        try {
            final var random = DECODER.decode(value.substring(0, separator));
            final var sealed = DECODER.decode(value.substring(separator + 1));
            return random.length == RANDOM_BYTES ? Optional.of(new InvitationLinkToken(random, sealed)) : Optional.empty();
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public byte[] lookupHash() {
        try {
            return MessageDigest.getInstance("SHA-256").digest(random);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public String encode() {
        return ENCODER.encodeToString(random) + SEPARATOR + ENCODER.encodeToString(sealedEmailHash);
    }

    /** Never the token itself: it would put a working link in the logs. */
    @Override
    public String toString() {
        return "InvitationLinkToken[redacted]";
    }
}
