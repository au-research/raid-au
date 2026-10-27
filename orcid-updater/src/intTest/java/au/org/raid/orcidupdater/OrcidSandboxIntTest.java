package au.org.raid.orcidupdater;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import au.org.raid.orcidupdater.orcid.MembershipFactory;
import au.org.raid.orcidupdater.orcid.OrcidMemberClient;
import au.org.raid.orcidupdater.orcid.OrcidRaid;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Contributor;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Position;
import au.org.raid.orcidupdater.orcid.OrcidRaid.Title;
import au.org.raid.orcidupdater.orcid.RorClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAID-1004 against the real ORCID sandbox and ROR, run by hand: writes a principal investigator
 * entry and a co-investigator entry, both naming the RAiD owner's organisation, to a sandbox
 * record, updates one, and prints what ORCID stored. Skipped unless ORCID_SANDBOX_TOKEN_FILE names
 * a JSON file holding {"orcid", "access_token"} for a sandbox iD; put codes are written next to it
 * so ORCID_SANDBOX_CLEANUP=true can delete the entries afterwards.
 */
// Gradle doesn't track environment variables: run with --rerun
@EnabledIfEnvironmentVariable(named = "ORCID_SANDBOX_TOKEN_FILE", matches = ".+")
@DisplayName("ORCID sandbox (manual)")
class OrcidSandboxIntTest {
    private static final String API = "https://api.sandbox.orcid.org/v3.0";
    private static final String OWNER = "https://ror.org/038sjwq14";
    private static final String TITLE = "https://vocabulary.raid.org/title.type.schema/5";
    private static final String PI = "https://vocabulary.raid.org/contributor.position.schema/307";
    private static final String CO_INVESTIGATOR = "https://vocabulary.raid.org/contributor.position.schema/308";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Write, update and read back sandbox entries, or delete them with ORCID_SANDBOX_CLEANUP=true")
    void sandbox() throws Exception {
        final var tokenFile = Path.of(System.getenv("ORCID_SANDBOX_TOKEN_FILE"));
        final var putCodesFile = tokenFile.resolveSibling("orcid-sandbox-put-codes.json");
        final var saved = objectMapper.readTree(tokenFile.toFile());
        final var orcidId = "https://sandbox.orcid.org/" + saved.get("orcid").asText();
        final var token = saved.get("access_token").asText();

        final var properties = new OrcidUpdaterProperties();
        properties.getOrcid().setApiUrl(API);
        final var client = new OrcidMemberClient(RestClient.builder(), properties, objectMapper);

        if ("true".equals(System.getenv("ORCID_SANDBOX_CLEANUP"))) {
            for (final var putCode : objectMapper.readTree(putCodesFile.toFile())) {
                client.delete(orcidId, token, putCode.asLong());
                System.out.println("Deleted " + putCode.asLong());
            }
            Files.delete(putCodesFile);
            return;
        }

        final var factory = new MembershipFactory(Clock.systemUTC());
        final var ror = new RorClient(RestClient.builder(), properties).find(OWNER);
        assertThat(ror).as("ROR lookup of ARDC").isPresent();

        final var piRaid = raid("10.82841/raid-1004-pi", "RAID-1004 sandbox check (principal investigator)", orcidId, PI);
        final var coRaid = raid("10.82841/raid-1004-co", "RAID-1004 sandbox check (co-investigator)", orcidId, CO_INVESTIGATOR);
        final var pi = piRaid.contributors().get(0);
        final var co = coRaid.contributors().get(0);

        final var piPutCode = client.create(orcidId, token, factory.create(piRaid, pi, null, ror.get()));
        final var coPutCode = client.create(orcidId, token, factory.create(coRaid, co, null, ror.get()));
        Files.writeString(putCodesFile, objectMapper.writeValueAsString(List.of(piPutCode, coPutCode)));
        // an update by put code, as every later save of the RAiD does
        client.update(orcidId, token, piPutCode, factory.create(piRaid, pi, piPutCode, ror.get()));

        final var reader = RestClient.builder().baseUrl(API)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.orcid+json").build();
        final var bareId = saved.get("orcid").asText();
        for (final var putCode : List.of(piPutCode, coPutCode)) {
            final var stored = reader.get().uri("/{id}/membership/{putCode}", bareId, putCode).retrieve().body(String.class);
            System.out.println("Membership " + putCode + ":\n"
                    + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(stored)));
        }
    }

    private static OrcidRaid raid(final String handle, final String title, final String orcidId, final String position) {
        return new OrcidRaid(handle, "https://raid.org/" + handle, OWNER,
                List.of(new Title(title, TITLE, "2025-03-01", null)),
                List.of(new Contributor(orcidId, List.of(new Position(position, "2025-03-01", "2027-02-28")))));
    }
}
