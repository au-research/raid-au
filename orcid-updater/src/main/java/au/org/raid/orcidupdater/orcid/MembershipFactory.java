package au.org.raid.orcidupdater.orcid;

import au.org.raid.orcidupdater.orcid.OrcidRaid.Contributor;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Position;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Title;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builds the ORCID v3.0 membership a RAiD becomes on a contributor's record, in ORCID's JSON
 * format (orcid-model record_3.0 write sample membership-3.0.json). The content matches what the
 * central updater wrote, so RAiD AU's existing entries are updated in place (RAID-1007).
 */
@Component
public class MembershipFactory {
    static final String PRIMARY_TITLE_TYPE = "https://vocabulary.raid.org/title.type.schema/5";
    /** The central updater's labels, so the role reads the same on existing entries. */
    static final Map<String, String> POSITION_LABELS = Map.of(
            "https://vocabulary.raid.org/contributor.position.schema/307", "Principal or Chief Investigator",
            "https://vocabulary.raid.org/contributor.position.schema/308", "Co-investigator or Collaborator",
            "https://vocabulary.raid.org/contributor.position.schema/309", "Partner Investigator",
            "https://vocabulary.raid.org/contributor.position.schema/310", "Consultant",
            "https://vocabulary.raid.org/contributor.position.schema/311", "Other Participant");

    private final Clock clock;

    public MembershipFactory(final Clock clock) {
        this.clock = clock;
    }

    /**
     * The contributor's current position: the one with the latest start date, as the central
     * updater chose it.
     */
    public Optional<Position> currentPosition(final Contributor contributor) {
        return contributor.positions().stream()
                .max(Comparator.comparing(position -> position.startDate() == null ? "" : position.startDate()));
    }

    /**
     * @param putCode      null to create the entry; ORCID's id for it to update it
     * @param organisation the RAiD owner's organisation. ORCID rejects a membership without one, and
     *                     a RAiD doesn't say which organisation each contributor belongs to (ADR,
     *                     RL 2026-10-27)
     */
    public Map<String, Object> create(final OrcidRaid raid, final Contributor contributor, final Long putCode,
                                      final OrcidOrganisation organisation) {
        final var membership = new LinkedHashMap<String, Object>();
        if (putCode != null) {
            membership.put("put-code", putCode);
        }
        membership.put("department-name", title(raid.titles()));
        currentPosition(contributor).ifPresent(position -> {
            membership.put("role-title", POSITION_LABELS.getOrDefault(position.id(), position.id()));
            date(position.startDate()).ifPresent(date -> membership.put("start-date", date));
            date(position.endDate()).ifPresent(date -> membership.put("end-date", date));
        });
        membership.put("organization", organisation(organisation));
        membership.put("url", Map.of("value", raid.url()));
        membership.put("external-ids", Map.of("external-id", List.of(Map.of(
                "external-id-type", "raid",
                "external-id-value", raid.url(),
                "external-id-url", Map.of("value", raid.url()),
                "external-id-relationship", "self"))));
        return membership;
    }

    /**
     * The current primary title, else any current title, else the primary title. The central
     * updater failed when no title was current.
     */
    String title(final List<Title> titles) {
        final var today = LocalDate.now(clock).toString();
        return titles.stream().filter(title -> current(title, today) && PRIMARY_TITLE_TYPE.equals(title.typeId())).findFirst()
                .or(() -> titles.stream().filter(title -> current(title, today)).findFirst())
                .or(() -> titles.stream().filter(title -> PRIMARY_TITLE_TYPE.equals(title.typeId())).findFirst())
                .or(() -> titles.stream().findFirst())
                .map(Title::text)
                .orElse("");
    }

    private static boolean current(final Title title, final String today) {
        // ISO dates compare correctly as text; a partial date is treated as the start of its period
        return (title.startDate() == null || title.startDate().compareTo(today) <= 0)
                && (title.endDate() == null || title.endDate().compareTo(today) > 0);
    }

    /** RAiD dates are YYYY, YYYY-MM or YYYY-MM-DD; ORCID takes each part separately. */
    static Optional<Map<String, Object>> date(final String date) {
        if (date == null) {
            return Optional.empty();
        }
        final var parts = date.split("-");
        final var orcidDate = new LinkedHashMap<String, Object>();
        orcidDate.put("year", Map.of("value", parts[0]));
        if (parts.length > 1) {
            orcidDate.put("month", Map.of("value", parts[1]));
        }
        if (parts.length > 2) {
            orcidDate.put("day", Map.of("value", parts[2]));
        }
        return Optional.of(orcidDate);
    }

    private static Map<String, Object> organisation(final OrcidOrganisation organisation) {
        final var address = new LinkedHashMap<String, Object>();
        address.put("city", organisation.city());
        if (organisation.region() != null) {
            address.put("region", organisation.region());
        }
        address.put("country", organisation.country());
        return Map.of(
                "name", organisation.name(),
                "address", address,
                "disambiguated-organization", Map.of(
                        "disambiguated-organization-identifier", organisation.rorId(),
                        "disambiguation-source", "ROR"));
    }
}
