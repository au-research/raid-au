package au.org.raid.orcidupdater.orcid;

import au.org.raid.orcidupdater.orcid.OrcidRaid.Contributor;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Position;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Title;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrcidRaid")
class OrcidRaidTest {
    @Test
    @DisplayName("Reads the identifier, owner, titles and contributors' positions from the RAiD API's JSON")
    void fromApi() throws Exception {
        final var json = new ObjectMapper().readTree("""
                {"identifier": {"id": "https://raid.org/10.82841/86bf1b6c",
                                "owner": {"id": "https://ror.org/038sjwq14", "servicePoint": 20000000}},
                 "title": [{"text": "Coral reef resilience", "type": {"id": "https://vocabulary.raid.org/title.type.schema/5"},
                            "startDate": "2025-03-01"}],
                 "contributor": [
                   {"id": "https://orcid.org/0000-0002-1825-0097", "status": "AUTHENTICATED",
                    "position": [{"id": "https://vocabulary.raid.org/contributor.position.schema/307",
                                  "startDate": "2025-03-01", "endDate": "2027-02-28"}]},
                   {"status": "UNAUTHENTICATED", "position": []}]}
                """);

        final var raid = OrcidRaid.fromApi("10.82841/86bf1b6c", json);

        assertThat(raid.handle()).isEqualTo("10.82841/86bf1b6c");
        assertThat(raid.url()).isEqualTo("https://raid.org/10.82841/86bf1b6c");
        assertThat(raid.ownerRorId()).isEqualTo("https://ror.org/038sjwq14");
        assertThat(raid.titles()).containsExactly(new Title("Coral reef resilience",
                "https://vocabulary.raid.org/title.type.schema/5", "2025-03-01", null));
        // a contributor without an iD can't have an ORCID entry
        assertThat(raid.contributors()).containsExactly(new Contributor("https://orcid.org/0000-0002-1825-0097",
                java.util.List.of(new Position("https://vocabulary.raid.org/contributor.position.schema/307",
                        "2025-03-01", "2027-02-28"))));
    }
}
