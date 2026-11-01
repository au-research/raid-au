package au.org.raid.orcidupdater.work;

import au.org.raid.orcidupdater.support.TestProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("WorkScheduler")
class WorkSchedulerTest {
    private final WorkProcessor processor = mock(WorkProcessor.class);
    private final WorkScheduler scheduler = new WorkScheduler(processor, TestProperties.complete());

    @Test
    @DisplayName("Keeps taking batches while they come back full, then waits for the next poll")
    void drainsBacklog() {
        when(processor.processDue()).thenReturn(20, 20, 7);

        scheduler.poll();

        verify(processor, times(3)).processDue();
    }

    @Test
    @DisplayName("A failed poll (e.g. database down) doesn't stop later polls")
    void survivesFailure() {
        when(processor.processDue()).thenThrow(new IllegalStateException("database down")).thenReturn(0);

        scheduler.poll();
        scheduler.poll();

        verify(processor, times(2)).processDue();
    }
}
