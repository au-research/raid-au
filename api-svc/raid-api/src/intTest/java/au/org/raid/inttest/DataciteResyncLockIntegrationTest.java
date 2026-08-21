package au.org.raid.inttest;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RAID-832 AC2: only one instance re-syncs per tick, coordinated by a Postgres session-level
 * advisory lock (see {@code DataciteResyncWorker.LOCK_KEY}). This can't be proven by the
 * worker's unit tests (which mock the DB), so exercise the real advisory lock semantics
 * against the test Postgres instance with two independent JDBC connections, mirroring how
 * two application instances would each hold their own connection for the whole tick.
 *
 * <p>This is a DB-level test that requires a real Postgres reachable at {@code localhost:7432}
 * (i.e. {@code ./gradlew dockerComposeUp} run locally). The black-box branch pipeline runs
 * intTest against a deployed API with no co-located Postgres, so this test is skipped there
 * via the assumption below rather than failing on a connection refused; the AC2 coverage it
 * gives is still exercised in the local intTest run.
 */
public class DataciteResyncLockIntegrationTest {

    // Same key as DataciteResyncWorker.LOCK_KEY - mirrored here because intTest has no
    // access to api-svc main classes (see reference_inttest_blackbox_classpath).
    private static final long LOCK_KEY = 0x2126_0832L;

    private static final String JDBC_HOST = "localhost";
    private static final int JDBC_PORT = 7432;
    private static final String JDBC_URL = "jdbc:postgresql://" + JDBC_HOST + ":" + JDBC_PORT + "/raido?currentSchema=api_svc";
    private static final String JDBC_USER = "postgres";
    private static final String JDBC_PASSWORD = "supersecret";

    @BeforeAll
    static void assumeLocalPostgresReachable() {
        Assumptions.assumeTrue(isLocalPostgresReachable(),
                "local Postgres (localhost:7432) not reachable; skipping DB-level RAID-832 integration test in black-box environment");
    }

    private static boolean isLocalPostgresReachable() {
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(JDBC_HOST, JDBC_PORT), 500);
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

    private static Connection newConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    private static boolean tryAdvisoryLock(final Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery("select pg_try_advisory_lock(" + LOCK_KEY + ")")) {
            resultSet.next();
            return resultSet.getBoolean(1);
        }
    }

    private static void advisoryUnlock(final Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery("select pg_advisory_unlock(" + LOCK_KEY + ")")) {
            resultSet.next();
        }
    }

    @Test
    @DisplayName("A second connection cannot acquire the resync advisory lock while the first holds it, but can once released")
    void secondConnectionBlockedThenAllowedAfterRelease() throws SQLException {
        try (var firstConnection = newConnection();
             var secondConnection = newConnection()) {

            // First "instance" acquires the lock for its tick.
            assertThat(tryAdvisoryLock(firstConnection))
                    .as("first connection should acquire the previously-unheld lock")
                    .isTrue();

            // A second "instance" ticking concurrently must not also acquire it.
            assertThat(tryAdvisoryLock(secondConnection))
                    .as("second connection must not acquire the lock while the first holds it")
                    .isFalse();

            // First instance finishes its tick and releases the lock.
            advisoryUnlock(firstConnection);

            // Now the second instance (or a later tick) can acquire it.
            assertThat(tryAdvisoryLock(secondConnection))
                    .as("lock should be acquirable again once released")
                    .isTrue();

            advisoryUnlock(secondConnection);
        }
    }
}
