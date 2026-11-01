package au.org.raid.orcidupdater.crypto;

import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailHasher")
class EmailHasherTest {
    private final EmailHasher hasher = new EmailHasher(TestProperties.complete());

    @Test
    @DisplayName("Trims and lowercases before hashing, so differently typed addresses match")
    void normalises() {
        assertThat(hasher.hash(" Researcher@Example.edu ")).isEqualTo(hasher.hash("researcher@example.edu"));
    }

    @Test
    @DisplayName("Different addresses give different fingerprints")
    void distinguishes() {
        assertThat(hasher.hash("a@example.edu")).isNotEqualTo(hasher.hash("b@example.edu"));
    }

    @Test
    @DisplayName("Keyed: another RA's secret gives a different fingerprint")
    void dependsOnSecret() {
        final var other = TestProperties.complete();
        other.setEmailHashSecret("another-secret");

        assertThat(new EmailHasher(other).hash("researcher@example.edu"))
                .isNotEqualTo(hasher.hash("researcher@example.edu"));
    }

    @Test
    @DisplayName("Is 32 bytes and does not contain the address")
    void doesNotContainAddress() {
        final var hash = hasher.hash("researcher@example.edu");

        assertThat(hash).hasSize(32);
        assertThat(new String(hash, StandardCharsets.ISO_8859_1)).doesNotContain("researcher");
    }
}
