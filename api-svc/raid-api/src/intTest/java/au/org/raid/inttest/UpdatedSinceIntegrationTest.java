package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.AccessType;
import au.org.raid.idl.raidv2.model.AccessTypeIdEnum;
import au.org.raid.idl.raidv2.model.AccessTypeSchemaUriEnum;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.inttest.dto.UserContext;
import au.org.raid.inttest.service.Handle;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static au.org.raid.fixtures.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * RAID-902: integration coverage for the "updatedSince" incremental-sync filter added across
 * RAID-897 (spec), RAID-898 (index), RAID-899 (filtering), RAID-900 (400 on malformed input) and
 * RAID-901 (raid-access-handler role for /raid/all-embargoed).
 */
@Slf4j
public class UpdatedSinceIntegrationTest extends AbstractIntegrationTest {

    @Value("${raid.test.api.url}")
    private String apiUrl;

    @Autowired
    private RestTemplate restTemplate;

    // ------------------------------------------------------------------
    // /raid/all-public
    // ------------------------------------------------------------------

    @Test
    @DisplayName("findAllPublicRaids(updatedSince) returns only raids updated after the cursor")
    void filteringOnAllPublic() {
        final var dumperContext = userService.createUser("raid-au", "raid-dumper");

        try {
            final var raidA = mintOpenAccessRaid();
            final var handleA = new Handle(raidA.getIdentifier().getId());
            final var cursorA = readUpdated(handleA);

            sleepPastSecondBoundary();

            final var raidB = mintOpenAccessRaid();
            final var handleB = new Handle(raidB.getIdentifier().getId());

            final var dumperApi = testClient.raidApi(dumperContext.getToken());
            final var results = dumperApi.findAllPublicRaids(toOffsetDateTime(cursorA)).getBody();
            assertThat(results).isNotNull();

            final var handles = handlesOf(results);

            assertThat(handles)
                    .as("filtering by raid A's updated cursor should include raid B, minted after it")
                    .contains(handleB.toString());
            assertThat(handles)
                    .as("filtering by raid A's own updated cursor should exclude raid A itself")
                    .doesNotContain(handleA.toString());
        } finally {
            userService.deleteUser(dumperContext.getId());
        }
    }

    @Test
    @DisplayName("findAllPublicRaids(updatedSince) is strictly-after: an exact match on the cursor is excluded")
    void filteringOnAllPublicExcludesExactBoundaryMatch() {
        final var dumperContext = userService.createUser("raid-au", "raid-dumper");

        try {
            final var raid = mintOpenAccessRaid();
            final var handle = new Handle(raid.getIdentifier().getId());
            final var cursor = readUpdated(handle);

            final var dumperApi = testClient.raidApi(dumperContext.getToken());
            final var results = dumperApi.findAllPublicRaids(toOffsetDateTime(cursor)).getBody();
            assertThat(results).isNotNull();

            assertThat(handlesOf(results))
                    .as("updatedSince filter is strictly-after (gt), so a raid whose metadata.updated "
                            + "exactly equals the supplied cursor must NOT be returned - this is the "
                            + "crux acceptance criterion of RAID-899's filtering behaviour")
                    .doesNotContain(handle.toString());
        } finally {
            userService.deleteUser(dumperContext.getId());
        }
    }

    @Test
    @DisplayName("findAllPublicRaids with no updatedSince preserves existing (unfiltered) behaviour")
    void omittedUpdatedSinceOnAllPublicReturnsAllRaids() {
        final var dumperContext = userService.createUser("raid-au", "raid-dumper");

        try {
            final var raidA = mintOpenAccessRaid();
            final var handleA = new Handle(raidA.getIdentifier().getId());

            sleepPastSecondBoundary();

            final var raidB = mintOpenAccessRaid();
            final var handleB = new Handle(raidB.getIdentifier().getId());

            final var dumperApi = testClient.raidApi(dumperContext.getToken());
            final var results = dumperApi.findAllPublicRaids(null).getBody();
            assertThat(results).isNotNull();

            final var handles = handlesOf(results);
            assertThat(handles)
                    .as("omitting updatedSince must remain backward compatible and return every public raid")
                    .contains(handleA.toString(), handleB.toString());
        } finally {
            userService.deleteUser(dumperContext.getId());
        }
    }

    // ------------------------------------------------------------------
    // Malformed updatedSince -> 400 (RAID-900)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Malformed updatedSince on /raid/all-public returns a structured 400")
    void malformedUpdatedSinceOnAllPublicReturns400() {
        final var dumperContext = userService.createUser("raid-au", "raid-dumper");

        try {
            final var uri = UriComponentsBuilder.fromHttpUrl(apiUrl + "/raid/all-public")
                    .queryParam("updatedSince", "not-a-timestamp")
                    .build()
                    .toUri();

            assertBadRequestForUpdatedSince(uri, dumperContext.getToken());
        } finally {
            userService.deleteUser(dumperContext.getId());
        }
    }

    @Test
    @DisplayName("updatedSince without a timezone offset on /raid/all-public returns a structured 400")
    void updatedSinceWithoutOffsetOnAllPublicReturns400() {
        final var dumperContext = userService.createUser("raid-au", "raid-dumper");

        try {
            final var uri = UriComponentsBuilder.fromHttpUrl(apiUrl + "/raid/all-public")
                    .queryParam("updatedSince", "2026-09-24T01:02:03")
                    .build()
                    .toUri();

            assertBadRequestForUpdatedSince(uri, dumperContext.getToken());
        } finally {
            userService.deleteUser(dumperContext.getId());
        }
    }

    private void assertBadRequestForUpdatedSince(final URI uri, final String token) {
        final var headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        final var entity = new HttpEntity<Void>(headers);

        try {
            restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            fail("Expected a 400 Bad Request for a malformed updatedSince value");
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());

            final var body = e.getResponseBodyAsString();
            assertThat(body).contains("updatedSince");
            assertThat(body).contains("ISO 8601");
            assertThat(body).contains("https://raid.org.au/errors#InvalidParameterFormat");
        }
    }

    // ------------------------------------------------------------------
    // Embargo -> open access transition bumps metadata.updated (RAID-899)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Changing access type from embargoed to open bumps metadata.updated and makes the raid visible via findAllPublicRaids")
    void embargoToOpenTransitionMakesRaidVisibleInAllPublic() {
        final var dumperContext = userService.createUser("raid-au", "raid-dumper");

        try {
            // createRequest's default fixture access is embargoed - see APIFixtures.newCreateRequest
            final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
            assertThat(mintedRaid).isNotNull();

            final var handle = new Handle(mintedRaid.getIdentifier().getId());
            final var cursor = readUpdated(handle);

            sleepPastSecondBoundary();

            final var current = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
            assertThat(current).isNotNull();

            final var updateRequest = raidUpdateRequestFactory.create(current);
            final var access = updateRequest.getAccess();
            access.setType(new AccessType()
                    .id(AccessTypeIdEnum.fromValue(OPEN_ACCESS_TYPE))
                    .schemaUri(AccessTypeSchemaUriEnum.fromValue(ACCESS_TYPE_SCHEMA_URI)));
            access.setEmbargoExpiry(null);
            access.setStatement(null);

            try {
                raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequest);
            } catch (Exception e) {
                failOnError(e);
            }

            final var dumperApi = testClient.raidApi(dumperContext.getToken());
            final var results = dumperApi.findAllPublicRaids(toOffsetDateTime(cursor)).getBody();
            assertThat(results).isNotNull();

            assertThat(handlesOf(results))
                    .as("an access-type-only change from embargoed to open must bump metadata.updated "
                            + "and the raid must now be visible (and public) via findAllPublicRaids")
                    .contains(handle.toString());
        } finally {
            userService.deleteUser(dumperContext.getId());
        }
    }

    // ------------------------------------------------------------------
    // /raid/all-embargoed (manual @GetMapping, not in the generated Feign client - RAID-901)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /raid/all-embargoed?updatedSince filters to raids updated after the cursor")
    void filteringOnAllEmbargoed() {
        final var accessHandlerContext = userService.createUser("raid-au", "raid-access-handler");

        try {
            // createRequest's default fixture access is embargoed
            final var raidA = raidApi.mintRaid(createRequest).getBody();
            assertThat(raidA).isNotNull();
            final var handleA = new Handle(raidA.getIdentifier().getId());
            final var cursorA = readUpdated(handleA);

            sleepPastSecondBoundary();

            final var raidB = raidApi.mintRaid(createRequest).getBody();
            assertThat(raidB).isNotNull();
            final var handleB = new Handle(raidB.getIdentifier().getId());

            final var handles = fetchAllEmbargoedHandles(accessHandlerContext, cursorA);

            assertThat(handles)
                    .as("filtering by raid A's updated cursor should include raid B, minted after it")
                    .contains(handleB.toString());
            assertThat(handles)
                    .as("filtering by raid A's own updated cursor should exclude raid A itself")
                    .doesNotContain(handleA.toString());
        } finally {
            userService.deleteUser(accessHandlerContext.getId());
        }
    }

    @Test
    @DisplayName("GET /raid/all-embargoed?updatedSince is strictly-after: an exact match on the cursor is excluded")
    void filteringOnAllEmbargoedExcludesExactBoundaryMatch() {
        final var accessHandlerContext = userService.createUser("raid-au", "raid-access-handler");

        try {
            final var raid = raidApi.mintRaid(createRequest).getBody();
            assertThat(raid).isNotNull();
            final var handle = new Handle(raid.getIdentifier().getId());
            final var cursor = readUpdated(handle);

            final var handles = fetchAllEmbargoedHandles(accessHandlerContext, cursor);

            assertThat(handles)
                    .as("updatedSince filter is strictly-after (gt), so an exact match on the cursor must be excluded")
                    .doesNotContain(handle.toString());
        } finally {
            userService.deleteUser(accessHandlerContext.getId());
        }
    }

    @Test
    @DisplayName("GET /raid/all-embargoed with no updatedSince preserves existing (unfiltered) behaviour")
    void omittedUpdatedSinceOnAllEmbargoedReturnsAllRaids() {
        final var accessHandlerContext = userService.createUser("raid-au", "raid-access-handler");

        try {
            final var raidA = raidApi.mintRaid(createRequest).getBody();
            assertThat(raidA).isNotNull();
            final var handleA = new Handle(raidA.getIdentifier().getId());

            sleepPastSecondBoundary();

            final var raidB = raidApi.mintRaid(createRequest).getBody();
            assertThat(raidB).isNotNull();
            final var handleB = new Handle(raidB.getIdentifier().getId());

            final var handles = fetchAllEmbargoedHandles(accessHandlerContext, null);

            assertThat(handles)
                    .as("omitting updatedSince must remain backward compatible and return every embargoed raid")
                    .contains(handleA.toString(), handleB.toString());
        } finally {
            userService.deleteUser(accessHandlerContext.getId());
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private RaidDto mintOpenAccessRaid() {
        createRequest.getAccess()
                .type(new AccessType()
                        .id(AccessTypeIdEnum.fromValue(OPEN_ACCESS_TYPE))
                        .schemaUri(AccessTypeSchemaUriEnum.fromValue(ACCESS_TYPE_SCHEMA_URI)))
                .statement(null)
                .embargoExpiry(null);

        final var minted = raidApi.mintRaid(createRequest).getBody();
        assertThat(minted).isNotNull();
        return minted;
    }

    private BigDecimal readUpdated(final Handle handle) {
        final var read = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assertThat(read).isNotNull();
        assertThat(read.getMetadata()).isNotNull();
        assertThat(read.getMetadata().getUpdated()).isNotNull();
        return read.getMetadata().getUpdated();
    }

    private OffsetDateTime toOffsetDateTime(final BigDecimal epochSeconds) {
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds.longValue()), ZoneOffset.UTC);
    }

    private void sleepPastSecondBoundary() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private List<String> handlesOf(final List<RaidDto> raids) {
        return raids.stream()
                .map(r -> new Handle(r.getIdentifier().getId()).toString())
                .toList();
    }

    private List<String> fetchAllEmbargoedHandles(final UserContext userContext, final BigDecimal cursorEpochSeconds) {
        final var builder = UriComponentsBuilder.fromHttpUrl(apiUrl + "/raid/all-embargoed");
        if (cursorEpochSeconds != null) {
            builder.queryParam("updatedSince", toOffsetDateTime(cursorEpochSeconds).toString());
        }
        final var uri = builder.build().encode().toUri();

        final var headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + userContext.getToken());
        final var entity = new HttpEntity<Void>(headers);

        final var response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        try {
            final List<RaidDto> raids = objectMapper.readValue(response.getBody(), new TypeReference<>() {
            });
            return handlesOf(raids);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse /raid/all-embargoed response body", e);
        }
    }
}
