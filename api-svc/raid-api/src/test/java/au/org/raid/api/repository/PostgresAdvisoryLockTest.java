package au.org.raid.api.repository;

import org.jooq.ConnectionCallable;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-tests only the try/finally-release contract on a mocked JDBC connection (release
 * happens even when {@code work} throws, and the lock isn't acquired on a fresh connection
 * before the "acquired" branch runs). Real Postgres advisory-lock semantics - mutual
 * exclusion between two genuinely different connections, and a waiter unblocking on release
 * - can't be proven against a mock and are instead covered by
 * {@code DataciteResyncLockIntegrationTest} against the real test database.
 */
@ExtendWith(MockitoExtension.class)
class PostgresAdvisoryLockTest {

    private static final long LOCK_KEY = 0x2126_0832L;

    @Mock
    private DSLContext db;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement lockStatement;
    @Mock
    private PreparedStatement unlockStatement;
    @Mock
    private ResultSet resultSet;

    private PostgresAdvisoryLock advisoryLock;

    @BeforeEach
    void setUp() throws Exception {
        advisoryLock = new PostgresAdvisoryLock(db);

        // Mirror DSLContext#connectionResult by handing the callable the single mock
        // connection, exactly like the worker's real single-held-connection contract.
        when(db.connectionResult(any())).thenAnswer(invocation -> {
            final ConnectionCallable<?> callable = invocation.getArgument(0);
            return callable.run(connection);
        });
    }

    @Test
    @DisplayName("runs work and releases the lock on the same connection when the lock is acquired")
    void runsWorkAndReleasesLockWhenAcquired() throws Exception {
        when(connection.prepareStatement("select pg_try_advisory_lock(?)")).thenReturn(lockStatement);
        when(connection.prepareStatement("select pg_advisory_unlock(?)")).thenReturn(unlockStatement);
        when(lockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getBoolean(1)).thenReturn(true);

        final var ran = new boolean[1];

        final var acquired = advisoryLock.runExclusively(LOCK_KEY, () -> ran[0] = true);

        assertThat(acquired).isTrue();
        assertThat(ran[0]).isTrue();
        verify(lockStatement).setLong(1, LOCK_KEY);
        verify(unlockStatement).setLong(1, LOCK_KEY);
        verify(unlockStatement).execute();
    }

    @Test
    @DisplayName("does not run work and returns false when the lock isn't acquired")
    void skipsWorkWhenNotAcquired() throws Exception {
        when(connection.prepareStatement("select pg_try_advisory_lock(?)")).thenReturn(lockStatement);
        when(lockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getBoolean(1)).thenReturn(false);

        final var ran = new boolean[1];

        final var acquired = advisoryLock.runExclusively(LOCK_KEY, () -> ran[0] = true);

        assertThat(acquired).isFalse();
        assertThat(ran[0]).isFalse();
        verify(connection, never()).prepareStatement("select pg_advisory_unlock(?)");
    }

    @Test
    @DisplayName("still releases the lock (in a finally) when work throws")
    void releasesLockEvenWhenWorkThrows() throws Exception {
        when(connection.prepareStatement("select pg_try_advisory_lock(?)")).thenReturn(lockStatement);
        when(connection.prepareStatement("select pg_advisory_unlock(?)")).thenReturn(unlockStatement);
        when(lockStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.getBoolean(1)).thenReturn(true);

        final Runnable work = () -> {
            throw new RuntimeException("work blew up");
        };

        assertThatThrownBy(() -> advisoryLock.runExclusively(LOCK_KEY, work))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("work blew up");

        verify(unlockStatement, times(1)).setLong(1, LOCK_KEY);
        verify(unlockStatement, times(1)).execute();
    }
}
