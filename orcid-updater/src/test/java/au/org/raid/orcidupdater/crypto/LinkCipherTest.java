package au.org.raid.orcidupdater.crypto;

import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LinkCipher")
class LinkCipherTest {
    private static final byte[] PLAINTEXT = "fingerprint-bytes".getBytes(StandardCharsets.UTF_8);
    private static final byte[] TOKEN = "token-a".getBytes(StandardCharsets.UTF_8);

    private final LinkCipher cipher = new LinkCipher(TestProperties.complete());

    @Test
    @DisplayName("Round trip with the same token")
    void roundTrip() {
        assertThat(cipher.decrypt(cipher.encrypt(PLAINTEXT, TOKEN), TOKEN)).contains(PLAINTEXT);
    }

    @Test
    @DisplayName("A random nonce means the same input never encrypts the same way twice")
    void nonDeterministic() {
        assertThat(cipher.encrypt(PLAINTEXT, TOKEN)).isNotEqualTo(cipher.encrypt(PLAINTEXT, TOKEN));
    }

    @Test
    @DisplayName("Does not open with another invitation's token")
    void boundToToken() {
        final var sealed = cipher.encrypt(PLAINTEXT, TOKEN);

        assertThat(cipher.decrypt(sealed, "token-b".getBytes(StandardCharsets.UTF_8))).isEmpty();
    }

    @Test
    @DisplayName("Does not open after any byte is changed")
    void detectsTampering() {
        final var sealed = cipher.encrypt(PLAINTEXT, TOKEN);
        sealed[sealed.length - 1] ^= 1;

        assertThat(cipher.decrypt(sealed, TOKEN)).isEmpty();
    }

    @Test
    @DisplayName("Truncated values do not open")
    void rejectsTruncated() {
        assertThat(cipher.decrypt(new byte[5], TOKEN)).isEmpty();
        assertThat(cipher.decrypt(new byte[0], TOKEN)).isEmpty();
    }

    @Test
    @DisplayName("Does not open with another RA's key")
    void boundToKey() {
        final var other = TestProperties.complete();
        other.setLinkEncryptionKey("a-different-key-also-at-least-32-characters");

        assertThat(new LinkCipher(other).decrypt(cipher.encrypt(PLAINTEXT, TOKEN), TOKEN)).isEmpty();
    }

    @Test
    @DisplayName("A short key stops startup, and the message gives the rule but not the key")
    void rejectsShortKey() {
        final var properties = TestProperties.complete();
        properties.setLinkEncryptionKey("too-short-secret");

        assertThatThrownBy(() -> new LinkCipher(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("orcid-updater.link-encryption-key")
                .hasMessageContaining("32")
                .hasMessageNotContaining("too-short-secret");
    }
}
