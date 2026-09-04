package au.org.raid.inttest.migration;

import au.org.raid.db.repair.RepairLog;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the one-shot repair bookkeeping behind {@code raid.db.repair-token},
 * including the situation it exists for: a schema history whose checksum no
 * longer matches the classpath, which makes every subsequent start fail.
 */
@Testcontainers
class RepairLogIntegrationTest {
    private static final String SCHEMA = "api_svc";

    /** A self-contained migration set, so this does not depend on the real one. */
    private static final String LOCATION = "classpath:db/repair-test";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    private static String jdbcUrl(final String database) {
        return POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + database);
    }

    private static DataSource dataSource(final String database) {
        final var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(jdbcUrl(database));
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }

    private static Connection connectionTo(final String database) throws SQLException {
        return DriverManager.getConnection(
                jdbcUrl(database), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static String freshDatabase(final String name) throws SQLException {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("drop database if exists " + name);
            statement.execute("create database " + name);
        }
        return name;
    }

    private static Flyway flyway(final String database) {
        return Flyway.configure()
                .dataSource(jdbcUrl(database), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations(LOCATION)
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .createSchemas(true)
                .load();
    }

    /**
     * Mirrors what {@code FlywayRepairConfig} does once a token is set, so the
     * repair-once behaviour is exercised end to end against a real database.
     */
    private static void migrateWithToken(final String database, final String token) throws SQLException {
        final var flyway = flyway(database);
        final var repairLog = new RepairLog(dataSource(database), SCHEMA, "flyway_schema_history");

        if (repairLog.shouldRepair(token)) {
            flyway.repair();
            repairLog.record(token);
        }
        flyway.migrate();
    }

    /**
     * Reproduces the situation a repair exists for. Editing an already-applied
     * migration changes its checksum, and every later start then fails validation.
     */
    private static void corruptStoredChecksum(final Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(
                    "update " + SCHEMA + ".flyway_schema_history set checksum = 1 where version = '1'");
        }
    }

    private static long queryLong(final Connection connection, final String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    @Test
    @DisplayName("repairs once for a token, then never again for that same token")
    void repairsOnceThenDisarms() throws SQLException {
        final var database = freshDatabase("repair_once");
        flyway(database).migrate();

        try (var connection = connectionTo(database)) {
            corruptStoredChecksum(connection);

            // Without a repair the instance cannot start at all.
            assertThatThrownBy(() -> flyway(database).migrate())
                    .hasMessageContaining("Validate failed");

            // Arming the token clears it in a single boot.
            assertThatNoException().isThrownBy(() -> migrateWithToken(database, "RAID-864"));
            assertThat(queryLong(connection, "select count(*) from " + SCHEMA + "." + RepairLog.TABLE_NAME
                    + " where token = 'RAID-864'")).isEqualTo(1);

            // Leaving the token set in deployment configuration is safe: it cannot
            // fire twice, so nobody has to remember to remove it.
            corruptStoredChecksum(connection);
            assertThatThrownBy(() -> migrateWithToken(database, "RAID-864"))
                    .as("the same token must not repair a second time")
                    .hasMessageContaining("Validate failed");

            // A different token is a new, deliberate decision, and does fire.
            assertThatNoException().isThrownBy(() -> migrateWithToken(database, "RAID-999"));
            assertThat(queryLong(connection,
                    "select count(*) from " + SCHEMA + "." + RepairLog.TABLE_NAME)).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("does not repair, or create anything, on a database that has never migrated")
    void doesNothingOnFreshDatabase() throws SQLException {
        final var database = freshDatabase("repair_fresh");

        // A token armed before anything has ever migrated must still let the
        // instance start. Creating the log here would leave a non-empty schema
        // with no history table, and Flyway would then refuse to migrate at all.
        assertThatNoException().isThrownBy(() -> migrateWithToken(database, "RAID-864"));

        try (var connection = connectionTo(database)) {
            assertThat(queryLong(connection,
                    "select count(*) from information_schema.tables where table_schema = '" + SCHEMA
                            + "' and table_name = '" + RepairLog.TABLE_NAME + "'"))
                    .as("there is no history to repair, so nothing should be created")
                    .isZero();

            // Migration still ran normally.
            assertThat(queryLong(connection, "select count(*) from " + SCHEMA + ".repair_probe")).isZero();
        }

        // And the token is still available to fire later, once there is history.
        final var repairLog = new RepairLog(dataSource(database), SCHEMA, "flyway_schema_history");
        assertThat(repairLog.shouldRepair("RAID-864")).isTrue();
    }

    @Test
    @DisplayName("records each distinct token separately as an audit trail")
    void recordsEachTokenSeparately() throws SQLException {
        final var database = freshDatabase("repair_audit");
        flyway(database).migrate();

        final var repairLog = new RepairLog(dataSource(database), SCHEMA, "flyway_schema_history");
        repairLog.record("RAID-100");
        repairLog.record("RAID-200");

        assertThat(repairLog.shouldRepair("RAID-100")).isFalse();
        assertThat(repairLog.shouldRepair("RAID-200")).isFalse();
        assertThat(repairLog.shouldRepair("RAID-300")).isTrue();

        try (var connection = connectionTo(database)) {
            assertThat(queryLong(connection, "select count(*) from " + SCHEMA + "." + RepairLog.TABLE_NAME
                    + " where repaired_at is not null")).isEqualTo(2);
        }
    }
}
