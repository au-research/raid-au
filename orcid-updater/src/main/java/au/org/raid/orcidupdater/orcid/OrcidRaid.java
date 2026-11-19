package au.org.raid.orcidupdater.orcid;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The parts of a RAiD the updater writes to ORCID, read from the RAiD API's JSON. Dates are kept
 * as the RAiD API gives them: YYYY, YYYY-MM or YYYY-MM-DD.
 *
 * @param url        the RAiD's identifier, e.g. https://raid.org/10.82841/86bf1b6c
 * @param ownerRorId the ROR of the organisation that owns the RAiD
 */
public record OrcidRaid(String handle, String url, String ownerRorId, List<Title> titles,
                        List<Contributor> contributors) {

    public record Title(String text, String typeId, String startDate, String endDate) {
    }

    /** @param id the ORCID iD as a URL */
    public record Contributor(String id, List<Position> positions) {
    }

    public record Position(String id, String startDate, String endDate) {
    }

    /** Reads a RAiD as returned by the RAiD API's GET /raid/{prefix}/{suffix}. */
    public static OrcidRaid fromApi(final String handle, final JsonNode raid) {
        final var titles = new ArrayList<Title>();
        raid.path("title").forEach(title -> titles.add(new Title(title.path("text").asText(""),
                title.path("type").path("id").asText(""), text(title, "startDate"), text(title, "endDate"))));

        final var contributors = new ArrayList<Contributor>();
        raid.path("contributor").forEach(contributor -> {
            final var id = text(contributor, "id");
            if (id == null) {
                return;
            }
            final var positions = new ArrayList<Position>();
            contributor.path("position").forEach(position -> positions.add(new Position(
                    position.path("id").asText(""), text(position, "startDate"), text(position, "endDate"))));
            contributors.add(new Contributor(id, positions));
        });

        final var identifier = raid.path("identifier");
        return new OrcidRaid(handle, identifier.path("id").asText(""), text(identifier.path("owner"), "id"),
                titles, contributors);
    }

    private static String text(final JsonNode node, final String field) {
        final var value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText() : null;
    }
}
