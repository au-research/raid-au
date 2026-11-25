package au.org.raid.orcidupdater.orcid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrcidIdRedactor")
class OrcidIdRedactorTest {
    @Test
    @DisplayName("Replaces bare iDs and iDs in URLs, including a final X check digit")
    void redactsIds() {
        assertThat(OrcidIdRedactor.redact(
                "PUT https://api.sandbox.orcid.org/v3.0/0000-0002-1825-0097/membership/1 for 0000-0002-9079-593X"))
                .isEqualTo("PUT https://api.sandbox.orcid.org/v3.0/[ORCID iD]/membership/1 for [ORCID iD]");
    }

    @Test
    @DisplayName("Leaves text without an iD, such as a RAiD handle or a put code, unchanged")
    void leavesOtherText() {
        assertThat(OrcidIdRedactor.redact("RAiD 10.82841/86bf1b6c: put code 1234567")).isEqualTo("RAiD 10.82841/86bf1b6c: put code 1234567");
    }

    @Test
    @DisplayName("Null stays null")
    void nullText() {
        assertThat(OrcidIdRedactor.redact(null)).isNull();
    }
}
