package au.org.raid.api.service.datacite;

import au.org.raid.api.config.properties.DataciteResyncProperties;
import au.org.raid.api.repository.DataciteResyncRepository;
import au.org.raid.api.repository.PostgresAdvisoryLock;
import au.org.raid.api.service.raid.RaidService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataciteResyncWorkerTest {

    // Mirrors DataciteResyncWorker.LOCK_KEY (private) so tests can assert tick() calls
    // through with the documented, stable key.
    private static final long LOCK_KEY = 0x2126_0832L;

    @Mock
    private DataciteResyncRepository repository;
    @Mock
    private RaidService raidService;
    @Mock
    private PostgresAdvisoryLock advisoryLock;

    private DataciteResyncProperties properties;

    private DataciteResyncWorker worker;

    @BeforeEach
    void setUp() {
        properties = new DataciteResyncProperties();
        properties.setBatchSize(50);
        properties.setThrottleMillis(0);
        properties.setPollDelayMillis(60000);
        worker = new DataciteResyncWorker(advisoryLock, repository, raidService, properties);
    }

    @Test
    @DisplayName("runBatch() does nothing when no raids are flagged")
    void noOpWhenNothingFlagged() {
        when(repository.findResyncRequired(properties.getBatchSize())).thenReturn(List.of());

        worker.runBatch();

        verifyNoInteractions(raidService);
        verify(repository, never()).clearResyncRequired(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("runBatch() re-pushes every flagged handle and clears the flag on success")
    void drainsBatchAndClearsFlagsOnSuccess() {
        final var handles = List.of("10.1234/abc", "10.1234/def", "10.1234/ghi");
        when(repository.findResyncRequired(properties.getBatchSize())).thenReturn(handles);

        worker.runBatch();

        for (final var handle : handles) {
            verify(raidService).resyncWithDatacite(handle);
            verify(repository).clearResyncRequired(handle);
        }
    }

    @Test
    @DisplayName("runBatch() leaves the flag set and continues when a re-push fails")
    void deferOnGenericFailure() {
        final var handles = List.of("10.1234/fails", "10.1234/succeeds");
        when(repository.findResyncRequired(properties.getBatchSize())).thenReturn(handles);

        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(raidService).resyncWithDatacite("10.1234/fails");

        worker.runBatch();

        verify(raidService).resyncWithDatacite("10.1234/fails");
        verify(repository, never()).clearResyncRequired("10.1234/fails");

        verify(raidService).resyncWithDatacite("10.1234/succeeds");
        verify(repository).clearResyncRequired("10.1234/succeeds");
    }

    @Test
    @DisplayName("runBatch() defers on a DataCite 429 like any other failure, logging it distinctly")
    void deferOnRateLimit() {
        final var handles = List.of("10.1234/throttled");
        when(repository.findResyncRequired(properties.getBatchSize())).thenReturn(handles);

        org.mockito.Mockito.doThrow(HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, null, null))
                .when(raidService).resyncWithDatacite("10.1234/throttled");

        worker.runBatch();

        verify(raidService).resyncWithDatacite("10.1234/throttled");
        verify(repository, never()).clearResyncRequired(eq("10.1234/throttled"));
    }

    @Test
    @DisplayName("runBatch() processes every handle in the batch, not just the first")
    void batchDrainingRespectsBatchSize() {
        properties.setBatchSize(2);
        final var handles = List.of("10.1234/one", "10.1234/two");
        when(repository.findResyncRequired(2)).thenReturn(handles);

        worker.runBatch();

        verify(raidService, times(2)).resyncWithDatacite(org.mockito.ArgumentMatchers.any());
        verify(repository, times(2)).clearResyncRequired(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("tick() skips the batch when the advisory lock isn't acquired")
    void tickSkipsBatchWhenLockNotAcquired() {
        // Mirror PostgresAdvisoryLock's real contract: not acquired => the work Runnable is
        // never invoked, and runExclusively returns false without running it.
        when(advisoryLock.runExclusively(eq(LOCK_KEY), any())).thenReturn(false);

        worker.tick();

        verifyNoInteractions(repository, raidService);
    }

    @Test
    @DisplayName("tick() runs the batch when the advisory lock is acquired")
    void tickRunsBatchWhenLockAcquired() {
        final var handles = List.of("10.1234/locked");
        when(repository.findResyncRequired(properties.getBatchSize())).thenReturn(handles);

        // Mirror PostgresAdvisoryLock's real contract: acquired => it invokes the work
        // Runnable itself (proving tick() wires runBatch() through correctly), then returns
        // true.
        when(advisoryLock.runExclusively(eq(LOCK_KEY), any())).thenAnswer(invocation -> {
            final Runnable work = invocation.getArgument(1);
            work.run();
            return true;
        });

        worker.tick();

        verify(raidService).resyncWithDatacite("10.1234/locked");
        verify(repository).clearResyncRequired("10.1234/locked");
    }

    @Test
    @DisplayName("tick() never propagates an exception out, so a bad tick can't kill the scheduler thread")
    void tickSwallowsExceptions() {
        when(advisoryLock.runExclusively(eq(LOCK_KEY), any()))
                .thenThrow(new RuntimeException("advisory lock blew up"));

        assertThatCode(worker::tick).doesNotThrowAnyException();
    }
}
