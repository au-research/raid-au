package au.org.raid.orcidupdater.crypto;

import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenCipher")
class TokenCipherTest {
    private static final String ORCID_ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String TOKEN = "f5af9f51-07e6-4332-8f1a-c0c11c1e3728";

    private final TokenCipher cipher = new TokenCipher(TestProperties.complete());

    @Test
    @DisplayName("Round trip for the same researcher")
    void roundTrip() {
        assertThat(cipher.decrypt(cipher.encrypt(TOKEN, ORCID_ID), ORCID_ID)).isEqualTo(TOKEN);
    }

    @Test
    @DisplayName("The stored value does not contain the token")
    void notPlaintext() {
        assertThat(new String(cipher.encrypt(TOKEN, ORCID_ID), StandardCharsets.ISO_8859_1)).doesNotContain(TOKEN);
    }

    @Test
    @DisplayName("A token copied to another researcher's row does not decrypt, and the error has no token")
    void boundToResearcher() {
        final var sealed = cipher.encrypt(TOKEN, ORCID_ID);

        assertThatThrownBy(() -> cipher.decrypt(sealed, "https://orcid.org/0000-0001-5109-3700"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining(TOKEN);
    }

    @Test
    @DisplayName("Uses a different key from the link cipher even if both settings hold the same secret")
    void separateFromLinkKey() {
        final var properties = TestProperties.complete();
        properties.setLinkEncryptionKey(properties.getTokenEncryptionKey());
        final var linkCipher = new LinkCipher(properties);
        final var orcidId = ORCID_ID.getBytes(StandardCharsets.UTF_8);

        assertThat(linkCipher.decrypt(new TokenCipher(properties).encrypt(TOKEN, ORCID_ID), orcidId)).isEmpty();
    }

    @Test
    @DisplayName("A short key fails startup, naming the setting but not the value")
    void shortKey() {
        final var properties = TestProperties.complete();
        properties.setTokenEncryptionKey("too-short-key-value");

        assertThatThrownBy(() -> new TokenCipher(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("orcid-updater.token-encryption-key")
                .hasMessageNotContaining("too-short-key-value");
    }
}
