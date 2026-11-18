package au.org.raid.api.service;

import au.org.raid.api.config.properties.ContributorStatusRefreshProperties;
import au.org.raid.api.exception.ResourceNotFoundException;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.api.repository.ContributorStatusRefreshRepository;
import au.org.raid.api.repository.ContributorStatusResetRepository;
import au.org.raid.api.repository.DataciteResyncRepository;
import au.org.raid.api.repository.PostgresAdvisoryLock;
import au.org.raid.api.service.raid.RaidService;
import au.org.raid.db.jooq.tables.records.ContributorRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributorStatusRefreshWorkerTest {
    private static final String CHANGED = "10.1/changed";
    private static final String UNCHANGED = "10.1/unchanged";
    private static final String ORCID = "https://orcid.org/0000-0001-0000-0001";

    @Mock
    private PostgresAdvisoryLock advisoryLock;
    @Mock
    private ContributorStatusResetRepository resetRepository;
    @Mock
    private ContributorRepository contributorRepository;
    @Mock
    private ContributorStatusService contributorStatusService;
    @Mock
    private ContributorStatusRefreshRepository repository;
    @Mock
    private DataciteResyncRepository dataciteResyncRepository;
    @Mock
    private RaidService raidService;

    private ContributorStatusRefreshProperties properties;
    private ContributorStatusRefreshWorker worker;

    @BeforeEach
    void setUp() {
        properties = new ContributorStatusRefreshProperties();
        properties.setThrottleMillis(0);
        worker = new ContributorStatusRefreshWorker(advisoryLock, resetRepository, contributorRepository,
                contributorStatusService, repository, dataciteResyncRepository, raidService, properties);
    }

    @BeforeEach
    void noResetsByDefault() {
        lenient().when(resetRepository.findPending(anyInt())).thenReturn(List.of());
    }

    private void stored(final String status) {
        when(contributorRepository.findByPid(ORCID)).thenReturn(Optional.of(new ContributorRecord().setStatus(status)));
    }

    @Test
    @DisplayName("Resets a recorded contributor through the status-change path, then marks them reset, before any DOI work")
    void resetsRecordedContributor() {
        when(resetRepository.findPending(properties.getBatchSize())).thenReturn(List.of(ORCID));
        stored("AUTHENTICATED");

        worker.runBatch();

        final var inOrder = inOrder(contributorStatusService, resetRepository);
        inOrder.verify(contributorStatusService).updateStatus(ORCID, "UNAUTHENTICATED");
        inOrder.verify(resetRepository).markReset(ORCID);
        verifyNoInteractions(repository, raidService);
    }

    @Test
    @DisplayName("Finishes a reset that only partly succeeded last time")
    void finishesPartialReset() {
        when(resetRepository.findPending(properties.getBatchSize())).thenReturn(List.of(ORCID));
        stored("UNAUTHENTICATED");

        worker.runBatch();

        verify(contributorStatusService).updateStatus(ORCID, "UNAUTHENTICATED");
        verify(resetRepository).markReset(ORCID);
    }

    @Test
    @DisplayName("Leaves a status the ORCID updater has set since, and marks the contributor done")
    void keepsUpdaterStatus() {
        when(resetRepository.findPending(properties.getBatchSize())).thenReturn(List.of(ORCID));
        stored("AWAITING_AUTHENTICATION");

        worker.runBatch();

        verifyNoInteractions(contributorStatusService);
        verify(resetRepository).markReset(ORCID);
    }

    @Test
    @DisplayName("A reset that fails stays pending for a later tick")
    void failedResetStaysPending() {
        when(resetRepository.findPending(properties.getBatchSize())).thenReturn(List.of(ORCID));
        stored("AUTHENTICATED");
        when(contributorStatusService.updateStatus(ORCID, "UNAUTHENTICATED"))
                .thenThrow(new ContributorStatusService.ContributorStatusRefreshException(1, 3));

        worker.runBatch();

        verify(resetRepository, never()).markReset(anyString());
    }

    @Test
    @DisplayName("Does nothing when no RAiD is flagged")
    void nothingFlagged() {
        when(repository.findRefreshRequired(properties.getBatchSize())).thenReturn(List.of());

        worker.runBatch();

        verifyNoInteractions(raidService, dataciteResyncRepository);
    }

    @Test
    @DisplayName("A RAiD whose statuses changed gets a new version through the refresh, and no extra DataCite update")
    void changedRaidIsVersioned() {
        when(repository.findRefreshRequired(properties.getBatchSize())).thenReturn(List.of(CHANGED));
        when(raidService.refreshContributorStatus(CHANGED)).thenReturn(true);

        worker.runBatch();

        verify(raidService, never()).resyncWithDatacite(anyString());
        verify(repository).clearRefreshRequired(CHANGED);
    }

    @Test
    @DisplayName("A RAiD whose statuses already agree keeps its version and gets a DataCite update alone")
    void unchangedRaidGetsDataciteOnly() {
        when(repository.findRefreshRequired(properties.getBatchSize())).thenReturn(List.of(UNCHANGED));
        when(raidService.refreshContributorStatus(UNCHANGED)).thenReturn(false);

        worker.runBatch();

        final var inOrder = inOrder(raidService, dataciteResyncRepository, repository);
        inOrder.verify(raidService).refreshContributorStatus(UNCHANGED);
        inOrder.verify(raidService).resyncWithDatacite(UNCHANGED);
        inOrder.verify(dataciteResyncRepository).clearResyncRequired(UNCHANGED);
        inOrder.verify(repository).clearRefreshRequired(UNCHANGED);
    }

    @Test
    @DisplayName("A failed DataCite-only update hands the RAiD to the resync worker and clears the refresh flag")
    void dataciteFailureFlagsResync() {
        when(repository.findRefreshRequired(properties.getBatchSize())).thenReturn(List.of(UNCHANGED));
        when(raidService.refreshContributorStatus(UNCHANGED)).thenReturn(false);
        doThrow(new IllegalStateException("DataCite down")).when(raidService).resyncWithDatacite(UNCHANGED);

        worker.runBatch();

        verify(dataciteResyncRepository).markResyncRequired(UNCHANGED);
        verify(dataciteResyncRepository, never()).clearResyncRequired(anyString());
        verify(repository).clearRefreshRequired(UNCHANGED);
    }

    @Test
    @DisplayName("A failing refresh keeps its flag for a later tick, and the rest of the batch carries on")
    void failureKeepsFlagAndContinues() {
        when(repository.findRefreshRequired(properties.getBatchSize())).thenReturn(List.of(CHANGED, UNCHANGED));
        when(raidService.refreshContributorStatus(CHANGED)).thenThrow(new IllegalStateException("conflict"));
        when(raidService.refreshContributorStatus(UNCHANGED)).thenReturn(true);

        worker.runBatch();

        verify(repository, never()).clearRefreshRequired(CHANGED);
        verify(repository).clearRefreshRequired(UNCHANGED);
    }

    @Test
    @DisplayName("A RAiD with no saved version is skipped and its flag cleared, since retrying can't help")
    void missingRaidSkipped() {
        when(repository.findRefreshRequired(properties.getBatchSize())).thenReturn(List.of(CHANGED));
        when(raidService.refreshContributorStatus(CHANGED)).thenThrow(new ResourceNotFoundException(CHANGED));

        worker.runBatch();

        verify(raidService, never()).resyncWithDatacite(anyString());
        verify(repository).clearRefreshRequired(CHANGED);
    }

    @Test
    @DisplayName("tick() runs the batch under the worker's own advisory lock, distinct from the resync worker's")
    void tickUsesOwnLock() {
        worker.tick();

        verify(advisoryLock).runExclusively(eq(ContributorStatusRefreshWorker.LOCK_KEY), any(Runnable.class));
        org.assertj.core.api.Assertions.assertThat(ContributorStatusRefreshWorker.LOCK_KEY).isNotEqualTo(0x2126_0832L);
    }

    @Test
    @DisplayName("tick() never lets a failure escape to the scheduler")
    void tickSwallowsFailure() {
        when(advisoryLock.runExclusively(anyLong(), any(Runnable.class))).thenThrow(new IllegalStateException("db down"));

        assertThatCode(worker::tick).doesNotThrowAnyException();
    }
}
