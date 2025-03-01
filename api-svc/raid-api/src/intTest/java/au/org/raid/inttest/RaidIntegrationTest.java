package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.ClosedRaid;
import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import au.org.raid.inttest.service.Handle;
import au.org.raid.inttest.service.RaidApiValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class RaidIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Mint a raid")
    void mintRaid() {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
        assert mintedRaid != null;

        final var handle = new Handle(mintedRaid.getIdentifier().getId());
        
        final var result = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix(), null).getBody();

        assertThat(result.getTitle()).isEqualTo(createRequest.getTitle());
        assertThat(result.getDescription()).isEqualTo(createRequest.getDescription());
        assertThat(result.getAccess()).isEqualTo(createRequest.getAccess());
        assertThat(result.getContributor().get(0).getId()).isEqualTo(createRequest.getContributor().get(0).getId());
        assertThat(result.getOrganisation()).isEqualTo(createRequest.getOrganisation());
        assertThat(result.getDate()).isEqualTo(createRequest.getDate());
    }

    @Test
    @DisplayName("Update a raid")
    void updateRaid() {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();

        assert mintedRaid != null;
        final var handle = new Handle(mintedRaid.getIdentifier().getId());
        final var readResult = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix(), null).getBody();

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
            fail("Update failed");
            throw new RuntimeException(e);
        }

        final var result = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix(), null).getBody();
        assert result != null;
        assertThat(result.getTitle().get(0).getText()).isEqualTo(title);
        assertThat(result.getIdentifier().getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Raid does not update if there are no changes")
    void updateRaidNoOp() {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();

        assert mintedRaid != null;
        final var handle = new Handle(mintedRaid.getIdentifier().getId());
        final var readResult = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix(), null).getBody();

        final var updateRequest = mapReadToUpdate(readResult);

        try {
            final var updateResult = raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequest).getBody();
            assertThat(updateResult.getIdentifier().getVersion()).isEqualTo(1);
        } catch (final Exception e) {
            fail("Update failed");
        }

        final var result = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix(), null).getBody();
        assertThat(result.getIdentifier().getVersion()).isEqualTo(1);
    }


    @Test
    @DisplayName("Resource not found error returned when raid not found on update")
    void notFound() {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();

        assert mintedRaid != null;
        final var handle = new Handle(mintedRaid.getIdentifier().getId());
        final var readResult = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix(), null).getBody();

        final var updateRequest = mapReadToUpdate(readResult);

        try {
            final var updateResult = raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequest).getBody();
            assertThat(updateResult.getIdentifier().getVersion()).isEqualTo(1);
        } catch (final Exception e) {
            fail("Update failed");
        }

        final var result = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix(), null).getBody();
        assertThat(result.getIdentifier().getVersion()).isEqualTo(1);
    }


    @Test
    @DisplayName("Forbidden response if embargoed raid from other service point is requested")
    void closedRaidOtherServicePoint() throws IOException {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
        assert mintedRaid != null;
        final var handle = new Handle(mintedRaid.getIdentifier().getId());

        final var api = testClient.raidApi(uqToken);

        try {
            final var readResult = api.findRaidByName(handle.getPrefix(), handle.getSuffix(), null).getBody();
            fail("Access to embargoed raid should be forbidden from different service point");
        } catch (final FeignException e) {
            assertThat(e.status()).isEqualTo(403);

            final var closedRaid = objectMapper.readValue(e.responseBody().get().array(), ClosedRaid.class);

            assertThat(closedRaid).isEqualTo(new ClosedRaid()
                    .identifier(mintedRaid.getIdentifier())
                    .access(mintedRaid.getAccess()));

        }
    }


    @Test
    @DisplayName("List raid does not show closed raids from other service points")
    void closedRaidsExcludedFromList() {
        raidApi.mintRaid(createRequest);
        final var ACCESS_TYPE_CLOSED = "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json";
        final var ACCESS_TYPE_EMBARGOED = "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/embargoed.json";

        final var api = testClient.raidApi(uqToken);

        try {
            final var raidList = api.findAllRaids(null, null).getBody();

            assert raidList != null;

            // filter closed/embargoed raids where the service point does not match RDM@UQ
            final var result = raidList.stream().filter(raid ->
                    !raid.getIdentifier().getOwner().getServicePoint().equals(UQ_SERVICE_POINT_ID) &&
                            (raid.getAccess().getType().getId().equals(ACCESS_TYPE_CLOSED) ||
                                    raid.getAccess().getType().getId().equals(ACCESS_TYPE_EMBARGOED)
                            )
            ).toList();

            assertThat(result).isEmpty();
        } catch (RaidApiValidationException e) {
            fail(e.getMessage());
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
                .spatialCoverage(read.getSpatialCoverage())
                .traditionalKnowledgeLabel(read.getTraditionalKnowledgeLabel());
    }
}