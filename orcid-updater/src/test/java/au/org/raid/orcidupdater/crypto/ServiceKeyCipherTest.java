package au.org.raid.orcidupdater.crypto;

import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ServiceKeyCipher")
class ServiceKeyCipherTest {
    private static final byte[] PRIVATE_KEY = "pkcs8-private-key-bytes".getBytes(StandardCharsets.UTF_8);

    private final ServiceKeyCipher cipher = new ServiceKeyCipher(TestProperties.complete());

    @Test
    @DisplayName("Round trip under the same key id, and the stored value isn't the key")
    void roundTrip() {
        final var sealed = cipher.encrypt(PRIVATE_KEY, "kid-1");

        assertThat(new String(sealed, StandardCharsets.ISO_8859_1)).doesNotContain("pkcs8-private-key");
        assertThat(cipher.decrypt(sealed, "kid-1")).contains(PRIVATE_KEY);
    }

    @Test
    @DisplayName("Bound to its key id")
    void boundToKeyId() {
        assertThat(cipher.decrypt(cipher.encrypt(PRIVATE_KEY, "kid-1"), "kid-2")).isEmpty();
    }

    @Test
    @DisplayName("After the token encryption key changes: empty, so a new key replaces it")
    void keyChanged() {
        final var properties = TestProperties.complete();
        properties.setTokenEncryptionKey("a-different-token-encryption-key-32-characters");

        assertThat(new ServiceKeyCipher(properties).decrypt(cipher.encrypt(PRIVATE_KEY, "kid-1"), "kid-1")).isEmpty();
    }

    @Test
    @DisplayName("Uses a different key from the token cipher, which shares the setting")
    void separateFromTokenCipher() {
        final var researcherTokenSealed = new TokenCipher(TestProperties.complete()).encrypt("token", "kid-1");

        assertThat(cipher.decrypt(researcherTokenSealed, "kid-1")).isEmpty();
    }
}
