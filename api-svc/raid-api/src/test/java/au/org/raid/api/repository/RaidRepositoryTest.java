package au.org.raid.api.repository;

import au.org.raid.api.config.properties.ContributorValidationProperties;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static au.org.raid.db.jooq.tables.Raid.RAID;
import static org.assertj.core.api.Assertions.assertThat;
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
        final var servicePointId = 10000000L;
        final var handles = List.of("10.26193/ABC123", "10.26193/DEF456");

        raidRepository.findAllViewable(servicePointId, true, handles);

        verify(dslContext).selectFrom(RAID);
    }

    @Test
    @DisplayName("findAllViewable() without service-point-user only includes open-access service point raids")
    void findAllViewableWithoutServicePointUserRole() {
        final var servicePointId = 10000000L;
        final var handles = List.of("10.26193/ABC123", "10.26193/DEF456");

        raidRepository.findAllViewable(servicePointId, false, handles);

        verify(dslContext).selectFrom(RAID);
    }

    @Test
    @DisplayName("findAllViewable() with service-point-user does not filter the service point's raids by access type, so embargoed raids are included (RAID-929)")
    void findAllViewableAsServicePointUserIncludesEmbargoed() {
        final var servicePointId = 10000000L;
        final var handles = List.of("10.26193/ABC123");

        raidRepository.findAllViewable(servicePointId, true, handles);

        final var condition = captureWhereCondition();
        assertThat(condition).contains("service_point_id").doesNotContain("access_type_id");
    }

    @Test
    @DisplayName("findAllViewable() without service-point-user restricts the service point's raids to open access (RAID-929)")
    void findAllViewableWithoutServicePointUserRoleExcludesEmbargoed() {
        final var servicePointId = 10000000L;
        final var handles = List.of("10.26193/ABC123");

        raidRepository.findAllViewable(servicePointId, false, handles);

        final var condition = captureWhereCondition();
        assertThat(condition).contains("service_point_id").contains("access_type_id");
    }

    private String captureWhereCondition() {
        final var captor = ArgumentCaptor.forClass(Condition.class);
        verify(dslContext.selectFrom(RAID)).where(captor.capture());
        return captor.getValue().toString();
    }

    @Test
    @DisplayName("findAllPublic(null) selects from raid directly, without an updatedSince filter")
    void findAllPublicWithoutUpdatedSince() {
        raidRepository.findAllPublic(null);

        verify(dslContext).selectFrom(RAID);
    }

    @Test
    @DisplayName("findAllPublic() with a non-null updatedSince still selects from raid directly")
    void findAllPublicWithUpdatedSince() {
        raidRepository.findAllPublic(BigDecimal.valueOf(1_700_000_000L));

        verify(dslContext).selectFrom(RAID);
    }

    @Test
    @DisplayName("findAllEmbargoed(null) selects from raid directly, without an updatedSince filter")
    void findAllEmbargoedWithoutUpdatedSince() {
        raidRepository.findAllEmbargoed(null);

        verify(dslContext).selectFrom(RAID);
    }

    @Test
    @DisplayName("findAllEmbargoed() with a non-null updatedSince still selects from raid directly")
    void findAllEmbargoedWithUpdatedSince() {
        raidRepository.findAllEmbargoed(BigDecimal.valueOf(1_700_000_000L));

        verify(dslContext).selectFrom(RAID);
    }

}
