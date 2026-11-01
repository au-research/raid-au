package au.org.raid.orcidupdater.orcid;

import au.org.raid.orcidupdater.orcid.OrcidRaid.Contributor;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Position;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Title;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MembershipFactory")
class MembershipFactoryTest {
    private static final String PRIMARY = MembershipFactory.PRIMARY_TITLE_TYPE;
    private static final String ALTERNATIVE = "https://vocabulary.raid.org/title.type.schema/4";
    private static final String PI = "https://vocabulary.raid.org/contributor.position.schema/307";
    private static final String CO_INVESTIGATOR = "https://vocabulary.raid.org/contributor.position.schema/308";
    private static final String ID = "https://orcid.org/0000-0002-1825-0097";
    private static final String URL = "https://raid.org/10.82841/86bf1b6c";
    private static final OrcidOrganisation ORGANISATION = new OrcidOrganisation("Australian Research Data Commons",
            "Melbourne", "Victoria", "AU", "https://ror.org/038sjwq14");

    private final MembershipFactory factory = new MembershipFactory(
            Clock.fixed(Instant.parse("2026-10-27T00:00:00Z"), ZoneOffset.UTC));

    private static OrcidRaid raid(final List<Title> titles, final Contributor contributor) {
        return new OrcidRaid("10.82841/86bf1b6c", URL, "https://ror.org/038sjwq14", titles, List.of(contributor));
    }

    private static Contributor contributor(final Position... positions) {
        return new Contributor(ID, List.of(positions));
    }

    @Test
    @DisplayName("Matches ORCID's JSON membership sample: title, role, dates, organisation, url and the RAiD as external id")
    void membership() throws Exception {
        final var contributor = contributor(new Position(CO_INVESTIGATOR, "2025-03-01", "2027-02-28"));
        final var mapper = new ObjectMapper();
        final var json = mapper.valueToTree(factory.create(
                raid(List.of(new Title("Coral reef resilience", PRIMARY, "2025-03-01", null)), contributor),
                contributor, null, ORGANISATION));

        // compared as JSON: key order doesn't matter to ORCID
        assertThat(json).isEqualTo(mapper.readTree("""
                {"department-name":"Coral reef resilience","role-title":"Co-investigator or Collaborator",\
                "start-date":{"year":{"value":"2025"},"month":{"value":"03"},"day":{"value":"01"}},\
                "end-date":{"year":{"value":"2027"},"month":{"value":"02"},"day":{"value":"28"}},\
                "organization":{"name":"Australian Research Data Commons",\
                "address":{"city":"Melbourne","region":"Victoria","country":"AU"},\
                "disambiguated-organization":{"disambiguated-organization-identifier":"https://ror.org/038sjwq14",\
                "disambiguation-source":"ROR"}},\
                "url":{"value":"https://raid.org/10.82841/86bf1b6c"},\
                "external-ids":{"external-id":[{"external-id-type":"raid",\
                "external-id-value":"https://raid.org/10.82841/86bf1b6c",\
                "external-id-url":{"value":"https://raid.org/10.82841/86bf1b6c"},\
                "external-id-relationship":"self"}]}}"""));
    }

    @Test
    @DisplayName("An update carries the put code; an organisation without a region leaves it out")
    void update() {
        final var contributor = contributor(new Position(PI, "2025-03-01", null));
        final var membership = factory.create(raid(List.of(new Title("T", PRIMARY, "2025-03-01", null)), contributor),
                contributor, 4242L, new OrcidOrganisation("Australian Research Data Commons", "Melbourne", null, "AU",
                        "https://ror.org/038sjwq14"));

        assertThat(membership).containsEntry("put-code", 4242L).doesNotContainKey("end-date");
        assertThat(membership.get("organization")).isEqualTo(Map.of(
                "name", "Australian Research Data Commons",
                "address", Map.of("city", "Melbourne", "country", "AU"),
                "disambiguated-organization", Map.of(
                        "disambiguated-organization-identifier", "https://ror.org/038sjwq14",
                        "disambiguation-source", "ROR")));
    }

    @Test
    @DisplayName("Role: the position with the latest start date; an unknown position is sent as its id")
    void role() {
        final var unknown = "https://vocabulary.raid.org/contributor.position.schema/999";
        final var contributor = contributor(new Position(PI, "2024-01-01", null), new Position(unknown, "2025-06-01", null));

        assertThat(factory.create(raid(List.of(), contributor), contributor, null, ORGANISATION))
                .containsEntry("role-title", unknown);
    }

    @Test
    @DisplayName("Title: current primary, else any current, else primary, else the first")
    void title() {
        final var oldPrimary = new Title("Old primary", PRIMARY, "2020-01-01", "2025-01-01");
        final var currentPrimary = new Title("Current primary", PRIMARY, "2025-01-01", null);
        final var currentAlternative = new Title("Current alternative", ALTERNATIVE, "2025-01-01", null);
        final var futureAlternative = new Title("Future alternative", ALTERNATIVE, "2030-01-01", null);

        assertThat(factory.title(List.of(currentAlternative, oldPrimary, currentPrimary))).isEqualTo("Current primary");
        assertThat(factory.title(List.of(oldPrimary, currentAlternative))).isEqualTo("Current alternative");
        assertThat(factory.title(List.of(futureAlternative, oldPrimary))).isEqualTo("Old primary");
        assertThat(factory.title(List.of(futureAlternative))).isEqualTo("Future alternative");
        assertThat(factory.title(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Partial RAiD dates become partial ORCID dates")
    void partialDates() {
        assertThat(MembershipFactory.date("2025")).contains(Map.of("year", Map.of("value", "2025")));
        assertThat(MembershipFactory.date("2025-03")).contains(Map.of(
                "year", Map.of("value", "2025"), "month", Map.of("value", "03")));
        assertThat(MembershipFactory.date(null)).isEmpty();
    }
}
