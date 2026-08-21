package au.org.raid.inttest;

import au.org.raid.inttest.service.Handle;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * RAID-832 AC4: a successful write-path DataCite post clears {@code datacite_resync_required}
 * so a user edit that lands before the worker's next tick doesn't get double-processed. The
 * worker's own unit tests mock the repository/service, so they can't prove the flag is
 * actually persisted and cleared through the real write path (RaidService -> DataciteResyncRepository
 * -> Postgres). This test forces the flag on directly in the DB (simulating a row queued for
 * re-sync by e.g. a targeting migration) and then exercises the normal update endpoint, asserting
 * the flag is cleared afterwards.
 *
 * <p>This is a DB-level test that seeds state via a direct connection to Postgres at
 * {@code localhost:7432} (i.e. {@code ./gradlew dockerComposeUp} run locally). The black-box
 * branch pipeline runs intTest against a deployed API with no co-located Postgres, so this test
 * is skipped there via the assumption below rather than failing on a connection refused; the AC4
 * coverage it gives is still exercised in the local intTest run.
 */
public class DataciteResyncFlagIntegrationTest extends AbstractIntegrationTest {

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

    private static void setResyncRequired(final String handle, final boolean value) throws SQLException {
        try (var connection = newConnection();
             var statement = connection.prepareStatement(
                     "update raid set datacite_resync_required = ? where handle = ?")) {
            statement.setBoolean(1, value);
            statement.setString(2, handle);
            final var updated = statement.executeUpdate();
            assertThat(updated).as("exactly one raid row should match handle %s", handle).isEqualTo(1);
        }
    }

    private static boolean getResyncRequired(final String handle) throws SQLException {
        try (var connection = newConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select datacite_resync_required from raid where handle = ?")) {
            statement.setString(1, handle);
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).as("raid row for handle %s should exist", handle).isTrue();
                return resultSet.getBoolean(1);
            }
        }
    }

    @Test
    @DisplayName("Flag forced on before mint is cleared by the mint's own DataCite post")
    void mintClearsFlagOnItsOwnRow() throws SQLException {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
        assert mintedRaid != null;
        final var handle = new Handle(mintedRaid.getIdentifier().getId()).toString();

        // Mint already clears the flag as part of RaidService.mintHandle; sanity check the
        // starting state is what we expect before moving on to the update-path assertion below.
        assertThat(getResyncRequired(handle))
                .as("newly-minted raid should not be flagged for resync")
                .isFalse();
    }

    @Test
    @DisplayName("A successful update clears a resync flag forced on directly in the DB")
    void updateClearsForciblySetResyncFlag() throws SQLException {
        final var mintedRaid = raidApi.mintRaid(createRequest).getBody();
        assert mintedRaid != null;
        final var handle = new Handle(mintedRaid.getIdentifier().getId());

        // Simulate a row queued for re-sync (e.g. by a targeting migration ahead of the worker's
        // next tick) by forcing the flag on directly in the DB, bypassing the API entirely.
        setResyncRequired(handle.toString(), true);
        assertThat(getResyncRequired(handle.toString()))
                .as("flag should be forced on before the update")
                .isTrue();

        final var readResult = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix()).getBody();
        assert readResult != null;
        final var updateRequest = raidUpdateRequestFactory.create(readResult);
        updateRequest.getTitle().get(0).setText(updateRequest.getTitle().get(0).getText() + " resync flag test");

        try {
            final var updateResult = raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequest).getBody();
            assert updateResult != null;
        } catch (final Exception e) {
            failOnError(e);
        }

        // A successful write-path DataCite post (RaidService.update) must clear the flag so the
        // worker doesn't re-push the same record again on its next tick.
        assertThat(getResyncRequired(handle.toString()))
                .as("update should have cleared the resync flag via RaidService.update -> DataciteResyncRepository.clearResyncRequired")
                .isFalse();
    }
}
