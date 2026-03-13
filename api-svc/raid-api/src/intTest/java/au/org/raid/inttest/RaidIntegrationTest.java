package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.*;
import au.org.raid.inttest.service.Handle;
import au.org.raid.inttest.service.RaidApiValidationException;
import static au.org.raid.fixtures.TestConstants.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Slf4j
public class RaidIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Mint a raid")
    void mintRaid() {
        try {
            final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
            assert mintedRaid != null;

            final var handle = new Handle(mintedRaid.getIdentifier().getId());

            final var result = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();

            assertThat(result.getTitle()).isEqualTo(createRequest.getTitle());
            assertThat(result.getDescription()).isEqualTo(createRequest.getDescription());
            assertThat(result.getAccess()).isEqualTo(createRequest.getAccess());
            //        assertThat(result.getContributor().get(0).getId()).isEqualTo(createRequest.getContributor().get(0).getId());
            //        assertThat(result.getContributor()).isEqualTo(createRequest.getContributor());
            assertThat(result.getOrganisation()).isEqualTo(createRequest.getOrganisation());
            assertThat(result.getDate()).isEqualTo(createRequest.getDate());
        } catch (final RaidApiValidationException e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Update a raid")
    void updateRaid() {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();

        assert mintedRaid != null;
        final var handle = new Handle(mintedRaid.getIdentifier().getId());
        final var readResult = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();

        assert readResult != null;
        final var updateRequest = mapReadToUpdate(readResult);

        final var title = updateRequest.getTitle().get(0).getText() + " updated";

        updateRequest.getTitle().get(0).setText(title);

        try {
            final var updateResult = raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequest).getBody();
            assert updateResult != null;
            assertThat(updateResult.getTitle().get(0).getText()).isEqualTo(title);
            assertThat(updateResult.getIdentifier().getVersion()).isEqualTo(2);
        } catch (final Exception e) {
            failOnError(e);
        }

        final var result = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assert result != null;
        assertThat(result.getTitle().get(0).getText()).isEqualTo(title);
        assertThat(result.getIdentifier().getVersion()).isEqualTo(2);
    }



    @Test
    @DisplayName("Patch a raid")
    void patchRaid() {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();

        assert mintedRaid != null;
        final var handle = new Handle(mintedRaid.getIdentifier().getId());
        final var readResult = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();

        final var contributor = readResult.getContributor().get(0);
        contributor.setId(REAL_TEST_ORCID);
        contributor.setStatus("AUTHENTICATED");

        final var patchRequest = new RaidPatchRequest().addContributorItem(contributor);
        try {
            final var responseEntity = raidApi.patchRaid(handle.getPrefix(), handle.getSuffix(), patchRequest);
            assert responseEntity != null;
            final var raid = responseEntity.getBody();
            assert raid != null;
            assertThat(raid.getContributor().get(0).getId()).isEqualTo(REAL_TEST_ORCID);
        } catch (final Exception e) {
            failOnError(e);
        }

        final var result = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assert result != null;
        assertThat(result.getContributor().get(0).getId()).isEqualTo(REAL_TEST_ORCID);
    }

    @Test
    @DisplayName("Raid does not update if there are no changes")
    void updateRaidNoOp() {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();

        assert mintedRaid != null;
        final var handle = new Handle(mintedRaid.getIdentifier().getId());
        final var readResult = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();

        final var updateRequest = mapReadToUpdate(readResult);

        try {
            final var updateResult = raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequest).getBody();
            assertThat(updateResult.getIdentifier().getVersion()).isEqualTo(1);
        } catch (final Exception e) {
            failOnError(e);
        }

        final var result = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assertThat(result.getIdentifier().getVersion()).isEqualTo(1);
    }


    @Test
    @DisplayName("Resource not found error returned when raid not found on update")
    void notFound() {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();

        assert mintedRaid != null;
        final var handle = new Handle(mintedRaid.getIdentifier().getId());
        final var readResult = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();

        final var updateRequest = mapReadToUpdate(readResult);

        try {
            final var updateResult = raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequest).getBody();
            assertThat(updateResult.getIdentifier().getVersion()).isEqualTo(1);
        } catch (final Exception e) {
            failOnError(e);
        }

        final var result = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assertThat(result.getIdentifier().getVersion()).isEqualTo(1);
    }


    @Test
    @DisplayName("Forbidden response if embargoed raid from other service point is requested")
    void closedRaidOtherServicePoint() throws IOException {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
        assert mintedRaid != null;
        final var handle = new Handle(mintedRaid.getIdentifier().getId());

        final var uqUserContext = userService.createUser("University of Queensland", "service-point-user");
        final var api = testClient.raidApi(uqUserContext.getToken());

        try {
            final var readResult = api.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
            fail("Access to embargoed raid should be forbidden from different service point");
        } catch (final FeignException e) {
            assertThat(e.status()).isEqualTo(403);
        } catch (final Exception e) {
            failOnError(e);
        } finally {
            userService.deleteUser(uqUserContext.getId());
        }
    }


    @Test
    @DisplayName("List raid does not show raids from other service points")
    void closedRaidsExcludedFromList() {
        raidApi.mintRaid(createRequest);

        final var uqUserContext = userService.createUser("University of Queensland", "service-point-user");

        final var api = testClient.raidApi(uqUserContext.getToken());

        try {
            final var raidList = api.findAllRaids(null, null, null).getBody();

            assert raidList != null;

            // filter closed/embargoed raids where the service point does not match RDM@UQ
            final var result = raidList.stream().filter(raid ->
                    !raid.getIdentifier().getOwner().getServicePoint().equals(UQ_SERVICE_POINT_ID)
            ).toList();

            assertThat(result).isEmpty();
        } catch (RaidApiValidationException e) {
            fail(e.getMessage());
        } finally {
            userService.deleteUser(uqUserContext.getId());
        }
    }

    @Disabled("Need a way to create contributors in tests")
    @Test
    @DisplayName("List all raids with a given contributor id")
    void listRaidsWithAGivenContributorId() {
        final String orcid = "https://orcid.org/0009-0006-4129-5257";

        final var user = userService.createUser(
                "raid-au",
                "pid-searcher", "service-point-user"
        );

        try {
            createRequest.getContributor().forEach(contributor -> {
                contributor.id(orcid);
                contributor.email(null);
                contributor.uuid(UUID.randomUUID().toString());
            });

            raidApi.mintRaid(createRequest);
            final var raidList = testClient
                    .raidApi(user.getToken()).findAllRaids(Collections.emptyList(), orcid, null).getBody();
            assert raidList != null;

            // find all raids in resultset that don't contain a contributor with the specified ORCID
            // there shouldn't be any
            final var erroneousRaids = raidList.stream()
                    .filter(raid -> !raid.getContributor().stream()
                            .map(Contributor::getId)
                            .toList()
                            .contains(orcid))
                    .toList();

            assertThat(raidList).isNotEmpty();
            assertThat(erroneousRaids).isEmpty();
        } catch (RaidApiValidationException e) {
            fail(e.getMessage());
        } finally {
            userService.deleteUser(user.getId());
        }
    }

    @Test
    @DisplayName("List all raids with a given organisation id")
    void listRaidsWithAGivenOrganisationId() {
        final String ror = "https://ror.org/039se3q37";

        final var user = userService.createUser(
                "raid-au",
                "pid-searcher", "service-point-user"
        );

        try {
            createRequest.getOrganisation().forEach(organisation -> organisation.id(ror));

            raidApi.mintRaid(createRequest);

            final var raidList = testClient
                    .raidApi(user.getToken()).findAllRaids(Collections.emptyList(), null, ror).getBody();
            assert raidList != null;

            // find all raids in resultset that don't contain a contributor with the specified ORCID
            // there shouldn't be any
            final var erroneousRaids = raidList.stream()
                    .filter(raid -> !raid.getOrganisation().stream()
                            .map(Organisation::getId)
                            .toList()
                            .contains(ror))
                    .toList();

            assertThat(raidList).isNotEmpty();
            assertThat(erroneousRaids).isEmpty();
        } catch (RaidApiValidationException e) {
            fail(e.getMessage());
        } finally {
            userService.deleteUser(user.getId());
        }
    }


    @Test
    @DisplayName("List all raids with a given organisation id should not return embargoed raids")
    void listRaidsWithAGivenOrganisationIdNoEmbargoedRaids() {
        final String ror = "https://ror.org/039se3q37";

        final var user = userService.createUser(
                "raid-au",
                "pid-searcher", "service-point-user"
        );

        try {
            createRequest.getOrganisation().forEach(organisation -> organisation.id(ror));
            createRequest.getAccess().getType().id(EMBARGOED_ACCESS_TYPE);
            createRequest.getAccess().getStatement().text("Embargoed");

            raidApi.mintRaid(createRequest);

            createRequest.getAccess().getType().id(OPEN_ACCESS_TYPE);
            createRequest.getAccess().getStatement().text("Open");
            createRequest.getAccess().embargoExpiry(null);

            raidApi.mintRaid(createRequest);

            final var raidList = testClient
                    .raidApi(user.getToken()).findAllRaids(Collections.emptyList(), null, ror).getBody();
            assert raidList != null;

            // find all raids in resultset that don't contain a contributor with the specified ORCID
            // there shouldn't be any
            final var erroneousRaids = raidList.stream()
                    .filter(raid -> !raid.getOrganisation().stream()
                            .map(Organisation::getId)
                            .toList()
                            .contains(ror))
                    .filter(raid -> !raid.getAccess().getType().getId().equals(EMBARGOED_ACCESS_TYPE))
                    .toList();

            assertThat(raidList).isNotEmpty();
            assertThat(erroneousRaids).isEmpty();
        } catch (RaidApiValidationException e) {
            fail(e.getMessage());
        } finally {
            userService.deleteUser(user.getId());
        }
    }


    @Test
    @DisplayName("Endpoint to show all public raids shouldn't show any embargoed raids")
    void findAllPublicShouldNotDisplayEmbargoedRaids() {
        final var user = userService.createUser(
                "raid-au",
                "raid-dumper"
        );

        try {
            raidApi.mintRaid(createRequest);

            createRequest.getAccess().getType().id(OPEN_ACCESS_TYPE);
            createRequest.getAccess().getStatement().text("Open");
            createRequest.getAccess().embargoExpiry(null);

            raidApi.mintRaid(createRequest);

            final var raidList = testClient.raidApi(user.getToken()).findAllPublicRaids().getBody();
            assert raidList != null;

            // find all raids in resultset that don't have an embargoed access type
            // there shouldn't be any
            final var erroneousRaids = raidList.stream()
                    .filter(raid -> raid.getAccess().getType().getId().equals(EMBARGOED_ACCESS_TYPE))
                    .toList();

            assertThat(raidList).isNotEmpty();
            assertThat(erroneousRaids).isEmpty();
        } catch (RaidApiValidationException e) {
            fail(e.getMessage());
        } finally {
            userService.deleteUser(user.getId());
        }
    }

    @Test
    @DisplayName("Find all public raids should only be available to raid-dumper")
    void findAllPublicPermissions() {
        final var user = userService.createUser(
                "raid-au",
                "service-point-user", "raid-user", "raid-admin"
        );

        try {
            raidApi.mintRaid(createRequest);

            createRequest.getAccess().getType().id(OPEN_ACCESS_TYPE);
            createRequest.getAccess().getStatement().text("Open");
            createRequest.getAccess().embargoExpiry(null);

            raidApi.mintRaid(createRequest);


            testClient.raidApi(user.getToken()).findAllPublicRaids().getBody();
        } catch (RaidApiValidationException e) {
            fail(e.getMessage());
        } catch (FeignException e) {
            assertThat(e.status()).isEqualTo(403);
        } finally {
            userService.deleteUser(user.getId());
        }
    }

    private RaidUpdateRequest mapReadToUpdate(RaidDto read) {
        return new RaidUpdateRequest()
                .metadata(read.getMetadata())
                .identifier(read.getIdentifier())
                .title(read.getTitle())
                .date(read.getDate())
                .description(read.getDescription())
                .access(read.getAccess())
                .alternateUrl(read.getAlternateUrl())
                .contributor(read.getContributor())
                .organisation(read.getOrganisation())
                .subject(read.getSubject())
                .relatedRaid(read.getRelatedRaid())
                .relatedObject(read.getRelatedObject())
                .alternateIdentifier(read.getAlternateIdentifier())
                .spatialCoverage(read.getSpatialCoverage());
    }
}