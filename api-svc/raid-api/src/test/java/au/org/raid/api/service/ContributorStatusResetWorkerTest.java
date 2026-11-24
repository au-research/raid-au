package au.org.raid.api.service;

import au.org.raid.api.config.properties.ContributorStatusResetProperties;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.api.repository.ContributorStatusResetRepository;
import au.org.raid.api.repository.PostgresAdvisoryLock;
import au.org.raid.db.jooq.tables.records.ContributorRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributorStatusResetWorkerTest {
    private static final String ORCID = "https://orcid.org/0000-0001-0000-0001";
    private static final String OTHER_ORCID = "https://orcid.org/0000-0001-0000-0002";

    @Mock
    private PostgresAdvisoryLock advisoryLock;
    @Mock
    private ContributorStatusResetRepository resetRepository;
    @Mock
    private ContributorRepository contributorRepository;
    @Mock
    private ContributorStatusService contributorStatusService;

    private ContributorStatusResetProperties properties;
    private ContributorStatusResetWorker worker;

    @BeforeEach
    void setUp() {
        properties = new ContributorStatusResetProperties();
        properties.setThrottleMillis(0);
        worker = new ContributorStatusResetWorker(advisoryLock, resetRepository, contributorRepository,
                contributorStatusService, properties);
    }

    private void stored(final String pid, final String status) {
        when(contributorRepository.findByPid(pid)).thenReturn(Optional.of(new ContributorRecord().setStatus(status)));
    }

    @Test
    @DisplayName("Resets a recorded contributor through the status-change path, then marks them reset")
    void resetsRecordedContributor() {
        when(resetRepository.findPending(properties.getBatchSize())).thenReturn(List.of(ORCID));
        stored(ORCID, "AUTHENTICATED");

        worker.runBatch();

        final var inOrder = inOrder(contributorStatusService, resetRepository);
        inOrder.verify(contributorStatusService).updateStatus(ORCID, "UNAUTHENTICATED");
        inOrder.verify(resetRepository).markReset(ORCID);
    }

    @Test
    @DisplayName("Finishes a reset that only partly succeeded last time")
    void finishesPartialReset() {
        when(resetRepository.findPending(properties.getBatchSize())).thenReturn(List.of(ORCID));
        stored(ORCID, "UNAUTHENTICATED");

        worker.runBatch();

        verify(contributorStatusService).updateStatus(ORCID, "UNAUTHENTICATED");
        verify(resetRepository).markReset(ORCID);
    }

    @Test
    @DisplayName("Leaves a status the ORCID updater has set since, and marks the contributor done")
    void keepsUpdaterStatus() {
        when(resetRepository.findPending(properties.getBatchSize())).thenReturn(List.of(ORCID));
        stored(ORCID, "AWAITING_AUTHENTICATION");

        worker.runBatch();

        verifyNoInteractions(contributorStatusService);
        verify(resetRepository).markReset(ORCID);
    }

    @Test
    @DisplayName("A recorded contributor with no row any more is marked done without a status change")
    void missingContributorMarkedDone() {
        when(resetRepository.findPending(properties.getBatchSize())).thenReturn(List.of(ORCID));
        when(contributorRepository.findByPid(ORCID)).thenReturn(Optional.empty());

        worker.runBatch();

        verifyNoInteractions(contributorStatusService);
        verify(resetRepository).markReset(ORCID);
    }

    @Test
    @DisplayName("A reset that fails stays pending for a later tick, and the rest of the batch carries on")
    void failedResetStaysPendingAndBatchContinues() {
        when(resetRepository.findPending(properties.getBatchSize())).thenReturn(List.of(ORCID, OTHER_ORCID));
        stored(ORCID, "AUTHENTICATED");
        stored(OTHER_ORCID, "AUTHENTICATED");
        when(contributorStatusService.updateStatus(ORCID, "UNAUTHENTICATED"))
                .thenThrow(new ContributorStatusService.ContributorStatusRefreshException(1, 3));

        worker.runBatch();

        verify(resetRepository, never()).markReset(ORCID);
        verify(contributorStatusService).updateStatus(OTHER_ORCID, "UNAUTHENTICATED");
        verify(resetRepository).markReset(OTHER_ORCID);
    }

    @Test
    @DisplayName("Does nothing once every recorded contributor is done")
    void nothingPending() {
        when(resetRepository.findPending(properties.getBatchSize())).thenReturn(List.of());

        worker.runBatch();

        verifyNoInteractions(contributorRepository, contributorStatusService);
        verify(resetRepository, never()).markReset(anyString());
    }

    @Test
    @DisplayName("tick() runs the batch under the worker's own advisory lock, distinct from the resync worker's")
    void tickUsesOwnLock() {
        worker.tick();

        verify(advisoryLock).runExclusively(eq(ContributorStatusResetWorker.LOCK_KEY), any(Runnable.class));
        assertThat(ContributorStatusResetWorker.LOCK_KEY).isNotEqualTo(0x2126_0832L);
    }

    @Test
    @DisplayName("tick() never lets a failure escape to the scheduler")
    void tickSwallowsFailure() {
        when(advisoryLock.runExclusively(anyLong(), any(Runnable.class))).thenThrow(new IllegalStateException("db down"));

        assertThatCode(worker::tick).doesNotThrowAnyException();
    }
}
