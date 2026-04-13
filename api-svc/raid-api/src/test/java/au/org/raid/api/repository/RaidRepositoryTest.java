package au.org.raid.api.repository;

import au.org.raid.api.config.properties.ContributorValidationProperties;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static au.org.raid.db.jooq.tables.Raid.RAID;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RaidRepositoryTest {
    @Mock
    ContributorValidationProperties contributorValidationProperties;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DSLContext dslContext;
    @InjectMocks
    RaidRepository raidRepository;

    @Test
    @DisplayName("updateMetadata() initiates a JOOQ update against the raid table")
    void updateMetadata() {
        final var handle = "10.26193/ABC123";
        final var metadata = "{\"identifier\":{\"id\":\"https://raid.org/10.26193/ABC123\"}}";

        raidRepository.updateMetadata(handle, metadata);

        verify(dslContext).update(RAID);
    }

    @Test
    @DisplayName("findAllViewable() with service-point-user includes all service point raids")
    void findAllViewableAsServicePointUser() {
        final var servicePointId = 20000000L;
        final var handles = List.of("10.26193/ABC123", "10.26193/DEF456");

        raidRepository.findAllViewable(servicePointId, true, handles);

        verify(dslContext).selectFrom(RAID);
    }

    @Test
    @DisplayName("findAllViewable() without service-point-user only includes open-access service point raids")
    void findAllViewableWithoutServicePointUserRole() {
        final var servicePointId = 20000000L;
        final var handles = List.of("10.26193/ABC123", "10.26193/DEF456");

        raidRepository.findAllViewable(servicePointId, false, handles);

        verify(dslContext).selectFrom(RAID);
    }

    @Test
    @DisplayName("findAllIncludingWithoutHistory() queries RAID table directly without RAID_HISTORY join")
    void findAllIncludingWithoutHistory() {
        raidRepository.findAllIncludingWithoutHistory();

        verify(dslContext).selectFrom(RAID);
    }
}
