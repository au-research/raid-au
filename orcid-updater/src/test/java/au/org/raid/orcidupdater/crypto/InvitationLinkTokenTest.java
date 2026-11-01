package au.org.raid.orcidupdater.crypto;

import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvitationLinkToken")
class InvitationLinkTokenTest {
    private static final byte[] EMAIL_HASH = "thirty-two-byte-email-fingerprint".getBytes(StandardCharsets.UTF_8);

    private final LinkCipher cipher = new LinkCipher(TestProperties.complete());

    @Test
    @DisplayName("Encodes to URL-safe text and parses back to the same token")
    void encodeAndParse() {
        final var token = InvitationLinkToken.issue(EMAIL_HASH, cipher);
        final var encoded = token.encode();

        assertThat(encoded).matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
        final var parsed = InvitationLinkToken.parse(encoded).orElseThrow();
        assertThat(parsed.random()).isEqualTo(token.random());
        assertThat(parsed.lookupHash()).isEqualTo(token.lookupHash());
    }

    @Test
    @DisplayName("The sealed part opens to the email fingerprint, with this token's random part only")
    void sealedEmailHashOpens() {
        final var token = InvitationLinkToken.issue(EMAIL_HASH, cipher);
        final var other = InvitationLinkToken.issue(EMAIL_HASH, cipher);

        assertThat(cipher.decrypt(token.sealedEmailHash(), token.random())).contains(EMAIL_HASH);
        assertThat(cipher.decrypt(token.sealedEmailHash(), other.random())).isEmpty();
    }

    @Test
    @DisplayName("Each token is different and its lookup hash is not the random part itself")
    void randomAndHashed() {
        final var a = InvitationLinkToken.issue(EMAIL_HASH, cipher);
        final var b = InvitationLinkToken.issue(EMAIL_HASH, cipher);

        assertThat(a.random()).hasSize(32).isNotEqualTo(b.random());
        assertThat(a.lookupHash()).hasSize(32).isNotEqualTo(a.random());
    }

    @Test
    @DisplayName("toString never prints the token")
    void redacted() {
        final var token = InvitationLinkToken.issue(EMAIL_HASH, cipher);

        assertThat(token.toString()).doesNotContain(token.encode().split("\\.")[0]);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "no-separator", ".abc", "abc.", "a.b.c", "!!!.abc", "c2hvcnQ.abc"})
    @DisplayName("Malformed values parse to nothing")
    void rejectsMalformed(final String value) {
        assertThat(InvitationLinkToken.parse(value)).isEmpty();
    }
}
