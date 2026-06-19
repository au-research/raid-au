package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.RaidDto;
import au.org.raid.idl.raidv2.model.RaidUpdateRequest;
import au.org.raid.inttest.service.Handle;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class MetadataCreatedIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("metadata.created is not overwritten on update")
    void metadataCreatedPreservedOnUpdate() {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
        assert mintedRaid != null;

        final var handle = new Handle(mintedRaid.getIdentifier().getId());
        final var readResult = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assert readResult != null;

        final var originalCreated = readResult.getMetadata().getCreated();
        assertThat(originalCreated).isNotNull();

        final var updateRequest = mapReadToUpdate(readResult);
        updateRequest.getTitle().get(0).setText("Updated title");

        try {
            raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequest);
        } catch (final Exception e) {
            failOnError(e);
        }

        final var updatedRaid = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assert updatedRaid != null;

        assertThat(updatedRaid.getMetadata().getCreated())
                .as("metadata.created should not change after update")
                .isEqualTo(originalCreated);
        assertThat(updatedRaid.getIdentifier().getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("metadata.created is not overwritten after multiple updates")
    void metadataCreatedPreservedAcrossMultipleUpdates() {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
        assert mintedRaid != null;

        final var handle = new Handle(mintedRaid.getIdentifier().getId());
        final var readResult = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assert readResult != null;

        final var originalCreated = readResult.getMetadata().getCreated();

        for (int i = 1; i <= 3; i++) {
            final var current = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
            assert current != null;

            final var updateRequest = mapReadToUpdate(current);
            updateRequest.getTitle().get(0).setText("Update " + i);

            try {
                raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequest);
            } catch (final Exception e) {
                failOnError(e);
            }
        }

        final var finalRaid = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assert finalRaid != null;

        assertThat(finalRaid.getMetadata().getCreated())
                .as("metadata.created should not change after multiple updates")
                .isEqualTo(originalCreated);
        assertThat(finalRaid.getIdentifier().getVersion()).isEqualTo(4);
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
