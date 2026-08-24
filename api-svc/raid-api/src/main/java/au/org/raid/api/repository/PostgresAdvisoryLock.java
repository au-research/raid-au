package au.org.raid.api.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Runs a unit of work exclusively across the fleet, coordinated by a Postgres
 * session-level advisory lock ({@code pg_try_advisory_lock}/{@code pg_advisory_unlock}).
 *
 * <p>The lock is acquired and released on the exact same JDBC connection, borrowed once from
 * {@link DSLContext#connectionResult} for the whole call - a session-level advisory lock is scoped
 * to the connection/session that took it, so releasing from a different (e.g. a different
 * pooled) connection would be a no-op and leak the lock. If the lock isn't acquired,
 * {@code work} is not run and this returns {@code false} immediately (no waiting for the
 * lock to free up). If it is acquired, {@code work} runs and the lock is released in a
 * {@code finally}, so it is released even if {@code work} throws.
 *
 * <p>Real Postgres advisory-lock semantics (mutual exclusion across two connections, and a
 * waiter being unblocked on release) are proven against a live database by
 * {@code DataciteResyncLockIntegrationTest} - a mocked JDBC connection can't meaningfully
 * exercise those semantics, so this class is unit tested only for the try/finally-release
 * contract (release still happens when {@code work} throws), not for real lock behaviour.
 */
@Component
@RequiredArgsConstructor
public class PostgresAdvisoryLock {

    private final DSLContext db;

    /**
     * Tries to acquire {@code lockKey} on a single dedicated connection; if acquired, runs
     * {@code work} then releases the lock (in a {@code finally}) before returning
     * {@code true}. If not acquired, returns {@code false} immediately without running
     * {@code work}.
     */
    public boolean runExclusively(final long lockKey, final Runnable work) {
        return Boolean.TRUE.equals(db.connectionResult(connection -> {
            if (!tryLock(connection, lockKey)) {
                return false;
            }
            try {
                work.run();
            } finally {
                unlock(connection, lockKey);
            }
            return true;
        }));
    }

    private boolean tryLock(final Connection connection, final long lockKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select pg_try_advisory_lock(?)")) {
            statement.setLong(1, lockKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    private void unlock(final Connection connection, final long lockKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select pg_advisory_unlock(?)")) {
            statement.setLong(1, lockKey);
            statement.execute();
        }
    }
}
