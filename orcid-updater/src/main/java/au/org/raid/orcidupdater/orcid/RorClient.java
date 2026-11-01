package au.org.raid.orcidupdater.orcid;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.regex.Pattern;

/** Looks up an organisation in ROR's v2 API for the organisation on an ORCID entry. */
@Slf4j
@Component
public class RorClient {
    private static final Pattern ROR_ID = Pattern.compile("^https://ror\\.org/(0[a-z0-9]{8})$");

    private final RestClient restClient;

    public RorClient(final RestClient.Builder builder, final OrcidUpdaterProperties properties) {
        this.restClient = builder.baseUrl(properties.getRor().getApiUrl()).build();
    }

    /**
     * @return the organisation, or empty when the id isn't a ROR, ROR has no such organisation or
     *         no city and country for it, or ROR couldn't be reached
     */
    public Optional<OrcidOrganisation> find(final String rorId) {
        final var matcher = rorId == null ? null : ROR_ID.matcher(rorId);
        if (matcher == null || !matcher.matches()) {
            log.warn("Not a ROR id, so the organisation for ORCID entries is unknown: {}", rorId);
            return Optional.empty();
        }
        final JsonNode organisation;
        try {
            organisation = restClient.get().uri("/organizations/{id}", matcher.group(1)).retrieve().body(JsonNode.class);
        } catch (final RestClientException e) {
            log.warn("ROR lookup failed for {}: {}", rorId, e.getMessage());
            return Optional.empty();
        }
        if (organisation == null) {
            return Optional.empty();
        }

        String name = null;
        for (final var candidate : organisation.path("names")) {
            for (final var type : candidate.path("types")) {
                if ("ror_display".equals(type.asText())) {
                    name = candidate.path("value").asText(null);
                }
            }
        }
        final var place = organisation.path("locations").path(0).path("geonames_details");
        final var city = place.path("name").asText(null);
        final var country = place.path("country_code").asText(null);
        if (name == null || city == null || country == null) {
            // ORCID requires a name, city and country on an organisation
            log.warn("ROR record for {} lacks a name, city or country", rorId);
            return Optional.empty();
        }
        return Optional.of(new OrcidOrganisation(name, city, place.path("country_subdivision_name").asText(null),
                country, rorId));
    }
}
