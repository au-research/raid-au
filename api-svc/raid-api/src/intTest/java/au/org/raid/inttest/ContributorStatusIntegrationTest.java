package au.org.raid.inttest;

import au.org.raid.idl.raidv2.api.ContributorApi;
import au.org.raid.idl.raidv2.model.ContributorStatusUpdate;
import au.org.raid.idl.raidv2.model.ContributorStatusUpdate.StatusEnum;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.inttest.dto.UserContext;
import au.org.raid.inttest.service.Handle;
import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static au.org.raid.fixtures.APIFixtures.orcidContributor;
import static au.org.raid.fixtures.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The ORCID updater sets a contributor's status with PUT /contributor/status. Every RAiD naming the
 * contributor takes the new status in a new version, and the DOI lists an ORCID contributor as a
 * creator only while they are AUTHENTICATED. Mint, update and patch ignore any status the client sends.
 *
 * <p>Status belongs to the person and is shared by every RAiD that names them, so these tests use an
 * iD that only they set status on, and take it off their RAiDs afterwards so a status change here only
 * ever touches the RAiD the test minted.
 */
public class ContributorStatusIntegrationTest extends AbstractIntegrationTest {
    private static final String STATUS_TEST_ORCID = "https://sandbox.orcid.org/0009-0005-9091-4416";
    private static final String STATUS_TEST_ORCID_DIGITS = "0009-0005-9091-4416";

    private static final String MOCKSERVER_HOST = "localhost";
    private static final int MOCKSERVER_PORT = 1080;
    private static final String MOCKSERVER_RETRIEVE_RECORDED_REQUESTS_URL =
            "http://" + MOCKSERVER_HOST + ":" + MOCKSERVER_PORT + "/mockserver/retrieve?type=REQUESTS";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${raid.test.api.url}")
    private String apiUrl;

    private UserContext contributorWriter;
    private ContributorApi contributorApi;
    private final List<Handle> minted = new ArrayList<>();

    @BeforeEach
    void setUpContributorWriter() {
        contributorWriter = userService.createUser("raid-au", "contributor-writer");
        contributorApi = testClient.contributorApi(contributorWriter.getToken());
    }

    @AfterEach
    void takeTestOrcidOffMintedRaids() {
        for (final var handle : minted) {
            try {
                final var raid = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
                final var update = raidUpdateRequestFactory.create(raid);
                update.setContributor(List.of(orcidContributor(
                        REAL_TEST_ORCID, PRINCIPAL_INVESTIGATOR_POSITION, SOFTWARE_CONTRIBUTOR_ROLE, today)));
                raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), update);
            } catch (final RuntimeException e) {
                // best effort: a leftover RAiD only makes later status changes slower
            }
        }
        userService.deleteUser(contributorWriter.getId());
    }

    private void setStatus(final StatusEnum status) {
        final var response = contributorApi.updateContributorStatus(
                new ContributorStatusUpdate().id(STATUS_TEST_ORCID).status(status));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    /** Mints a RAiD whose only contributor is the test iD, with the client claiming it's AUTHENTICATED. */
    private Handle mintWithTestOrcid() {
        createRequest.getTitle().get(0).setText("contributor-status-" + UUID.randomUUID());
        createRequest.setContributor(List.of(orcidContributor(
                STATUS_TEST_ORCID, PRINCIPAL_INVESTIGATOR_POSITION, SOFTWARE_CONTRIBUTOR_ROLE, today)
                .status("AUTHENTICATED")));

        final var handle = new Handle(raidApi.mintRaid(createRequest).getBody().getIdentifier().getId());
        minted.add(handle);
        return handle;
    }

    private RaidDto read(final Handle handle) {
        return raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
    }

    @Test
    @DisplayName("A status change gives the RAiD a new version holding it, and repeating it changes nothing")
    void statusChangeCreatesNewVersion() {
        setStatus(StatusEnum.UNAUTHENTICATED);
        final var handle = mintWithTestOrcid();

        final var minted = read(handle);
        assertThat(minted.getContributor().get(0).getStatus())
                .as("mint ignores the status the client sent")
                .isEqualTo("UNAUTHENTICATED");
        final var mintedVersion = minted.getIdentifier().getVersion();

        setStatus(StatusEnum.AUTHENTICATED);

        final var authenticated = read(handle);
        assertThat(authenticated.getContributor().get(0).getStatus()).isEqualTo("AUTHENTICATED");
        assertThat(authenticated.getIdentifier().getVersion()).isEqualTo(mintedVersion + 1);

        setStatus(StatusEnum.AUTHENTICATED);

        assertThat(read(handle).getIdentifier().getVersion())
                .as("the same status again creates no version")
                .isEqualTo(mintedVersion + 1);

        setStatus(StatusEnum.AUTHENTICATION_REVOKED);

        final var revoked = read(handle);
        assertThat(revoked.getContributor().get(0).getStatus()).isEqualTo("AUTHENTICATION_REVOKED");
        assertThat(revoked.getIdentifier().getVersion()).isEqualTo(mintedVersion + 2);
    }

    @Test
    @DisplayName("An update can't change a contributor's status")
    void updateIgnoresClientStatus() {
        setStatus(StatusEnum.AUTHENTICATED);
        final var handle = mintWithTestOrcid();

        final var update = raidUpdateRequestFactory.create(read(handle));
        update.getTitle().get(0).setText("contributor-status-updated-" + UUID.randomUUID());
        update.getContributor().get(0).setStatus("UNAUTHENTICATED");
        raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), update);

        assertThat(read(handle).getContributor().get(0).getStatus()).isEqualTo("AUTHENTICATED");
    }

    @Test
    @DisplayName("The DOI lists an ORCID contributor only while they are AUTHENTICATED")
    void doiCreatorsFollowStatus() {
        Assumptions.assumeTrue(isMockServerReachable(), "mockserver not reachable - skipping DataCite creator check");

        setStatus(StatusEnum.AWAITING_AUTHENTICATION);
        final var handle = mintWithTestOrcid();

        assertThat(latestCreators(HttpMethod.POST, "/dois", handle.toString()))
                .as("not on the DOI before authenticating")
                .doesNotContain(STATUS_TEST_ORCID_DIGITS);

        setStatus(StatusEnum.AUTHENTICATED);

        assertThat(latestCreators(HttpMethod.PUT, "/dois/" + handle, handle.toString()))
                .as("on the DOI once authenticated")
                .contains(STATUS_TEST_ORCID_DIGITS);

        setStatus(StatusEnum.AUTHENTICATION_REVOKED);

        assertThat(latestCreators(HttpMethod.PUT, "/dois/" + handle, handle.toString()))
                .as("off the DOI once revoked")
                .doesNotContain(STATUS_TEST_ORCID_DIGITS);
    }

    @Test
    @DisplayName("An unknown contributor is 404")
    void unknownContributor() {
        final var update = new ContributorStatusUpdate()
                .id("https://sandbox.orcid.org/unknown-" + UUID.randomUUID())
                .status(StatusEnum.AUTHENTICATED);

        assertThatThrownBy(() -> contributorApi.updateContributorStatus(update))
                .isInstanceOfSatisfying(FeignException.class, e -> assertThat(e.status()).isEqualTo(404));
    }

    @Test
    @DisplayName("Only the contributor-writer role may set status")
    void servicePointUserForbidden() {
        final var asServicePointUser = testClient.contributorApi(userContext.getToken());
        final var update = new ContributorStatusUpdate().id(STATUS_TEST_ORCID).status(StatusEnum.AUTHENTICATED);

        assertThatThrownBy(() -> asServicePointUser.updateContributorStatus(update))
                .isInstanceOfSatisfying(FeignException.class, e -> assertThat(e.status()).isEqualTo(403));
    }

    @Test
    @DisplayName("An unrecognised status is 400")
    void invalidStatus() {
        assertThat(putRawStatus("{\"id\":\"" + STATUS_TEST_ORCID + "\",\"status\":\"VERIFIED\"}"))
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("A missing id is 400")
    void missingId() {
        assertThat(putRawStatus("{\"status\":\"AUTHENTICATED\"}")).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /** Sends a body the generated client can't build, such as a status outside the enum. */
    private HttpStatus putRawStatus(final String body) {
        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(contributorWriter.getToken());

        try {
            final var response = restTemplate.exchange(apiUrl + "/contributor/status", HttpMethod.PUT,
                    new HttpEntity<>(body, headers), Void.class);
            return HttpStatus.valueOf(response.getStatusCode().value());
        } catch (final HttpClientErrorException e) {
            return HttpStatus.valueOf(e.getStatusCode().value());
        }
    }

    private static boolean isMockServerReachable() {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(MOCKSERVER_HOST, MOCKSERVER_PORT), 2000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** The creators of the most recent DataCite request recorded for the method and path that names the handle. */
    private String latestCreators(final HttpMethod method, final String path, final String handle) {
        final var matcher = objectMapper.createObjectNode();
        matcher.put("path", path);
        matcher.put("method", method.name());

        final var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        final var recorded = restTemplate.exchange(MOCKSERVER_RETRIEVE_RECORDED_REQUESTS_URL, HttpMethod.PUT,
                new HttpEntity<>(matcher.toString(), headers), JsonNode.class).getBody();

        JsonNode latest = null;
        if (recorded != null) {
            for (final var request : recorded) {
                final var body = jsonBody(request);
                if (body != null && body.toString().contains(handle)) {
                    latest = body;
                }
            }
        }

        assertThat(latest)
                .as("Expected a recorded DataCite %s %s request naming %s", method, path, handle)
                .isNotNull();

        return latest.path("data").path("attributes").path("creators").toString();
    }

    private JsonNode jsonBody(final JsonNode recordedRequest) {
        final var body = recordedRequest.path("body");
        if (body.has("json")) {
            return body.get("json");
        }
        if (body.has("string")) {
            try {
                return objectMapper.readTree(body.get("string").asText());
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }
}
