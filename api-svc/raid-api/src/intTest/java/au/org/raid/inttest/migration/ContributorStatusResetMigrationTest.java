package au.org.raid.inttest.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for V50, which records every AUTHENTICATED ORCID contributor so the API can reset
 * them. It changes no status: the API resets each one through the status-change path, so their RAiDs
 * stay in step.
 *
 * <p>Runs against a throwaway Postgres so it can seed contributors freely.
 */
@Testcontainers
class ContributorStatusResetMigrationTest {
    private static final String SCHEMA = "api_svc";
    private static final String BEFORE_RESET = "49";
    private static final String RESET = "50";

    private static final String ORCID = "https://orcid.org/";
    private static final String SANDBOX_ORCID = "https://sandbox.orcid.org/";
    private static final String ISNI = "https://isni.org/";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    private static Flyway flyway(final String database, final String target) {
        return Flyway.configure()
                .dataSource(urlFor(database), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .createSchemas(true)
                .baselineVersion("25")
                .target(target)
                .placeholders(Map.of(
                        "servicePointIdStart", "20000000",
                        "servicePointIdBlockSize", "10000000"))
                .load();
    }

    private static String urlFor(final String database) {
        return POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + database);
    }

    private static Connection connectionTo(final String database) throws SQLException {
        final var connection = DriverManager.getConnection(
                urlFor(database), POSTGRES.getUsername(), POSTGRES.getPassword());
        try (var statement = connection.createStatement()) {
            statement.execute("set search_path to " + SCHEMA);
        }
        return connection;
    }

    /** Each case gets its own database so migration history can't leak between them. */
    private static String freshDatabase(final String name) throws SQLException {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("drop database if exists " + name);
            statement.execute("create database " + name);
        }
        return name;
    }

    private static void ensureSchema(final Connection connection, final String uri) throws SQLException {
        try (var statement = connection.prepareStatement("""
                insert into contributor_schema (uri)
                select ? where not exists (select 1 from contributor_schema where uri = ?)
                """)) {
            statement.setString(1, uri);
            statement.setString(2, uri);
            statement.executeUpdate();
        }
    }

    private static void seedContributor(final Connection connection, final String schemaUri, final String pid,
                                        final String status) throws SQLException {
        try (var statement = connection.prepareStatement("""
                insert into contributor (pid, schema_id, status)
                values (?, (select id from contributor_schema where uri = ?), ?)
                """)) {
            statement.setString(1, pid);
            statement.setString(2, schemaUri);
            statement.setString(3, status);
            statement.executeUpdate();
        }
    }

    private static Map<String, String> statuses(final Connection connection) throws SQLException {
        final var statuses = new HashMap<String, String>();
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("select pid, status from contributor")) {
            while (rs.next()) {
                statuses.put(rs.getString(1), rs.getString(2));
            }
        }
        return statuses;
    }

    private static Map<String, String> retained(final Connection connection) throws SQLException {
        final var retained = new HashMap<String, String>();
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("select pid, schema_uri, previous_status from contributor_status_reset")) {
            while (rs.next()) {
                assertThat(rs.getString(3)).isEqualTo("AUTHENTICATED");
                retained.put(rs.getString(1), rs.getString(2));
            }
        }
        return retained;
    }

    private static void runMigrationSql(final Connection connection) throws Exception {
        final var sql = new String(ContributorStatusResetMigrationTest.class.getClassLoader()
                .getResourceAsStream("db/migration/V50__reset_authenticated_contributors.sql").readAllBytes());
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    @Test
    @DisplayName("Records every AUTHENTICATED ORCID contributor as not yet reset, and changes no status")
    void resetsAuthenticatedOrcidContributors() throws Exception {
        final var database = freshDatabase("contributor_reset");
        flyway(database, BEFORE_RESET).migrate();

        try (var connection = connectionTo(database)) {
            ensureSchema(connection, ORCID);
            ensureSchema(connection, SANDBOX_ORCID);
            ensureSchema(connection, ISNI);

            seedContributor(connection, ORCID, "0000-0001-0000-0001", "AUTHENTICATED");
            seedContributor(connection, SANDBOX_ORCID, "0000-0001-0000-0002", "AUTHENTICATED");
            seedContributor(connection, ORCID, "0000-0001-0000-0003", "AWAITING_AUTHENTICATION");
            seedContributor(connection, ORCID, "0000-0001-0000-0004", "AUTHENTICATION_REVOKED");
            seedContributor(connection, ORCID, "0000-0001-0000-0005", null);
            seedContributor(connection, ISNI, "0000-0001-0000-0006", "AUTHENTICATED");
        }

        flyway(database, RESET).migrate();

        try (var connection = connectionTo(database)) {
            final var statuses = statuses(connection);
            assertThat(statuses)
                    .as("the migration changes no status; the API resets each contributor and their RAiDs together")
                    .containsEntry("0000-0001-0000-0001", "AUTHENTICATED")
                    .containsEntry("0000-0001-0000-0002", "AUTHENTICATED")
                    .containsEntry("0000-0001-0000-0003", "AWAITING_AUTHENTICATION")
                    .containsEntry("0000-0001-0000-0004", "AUTHENTICATION_REVOKED")
                    .containsEntry("0000-0001-0000-0005", null)
                    .containsEntry("0000-0001-0000-0006", "AUTHENTICATED");

            assertThat(retained(connection))
                    .as("ISNI contributors don't authenticate, so they aren't recorded")
                    .containsOnly(
                            Map.entry("0000-0001-0000-0001", ORCID),
                            Map.entry("0000-0001-0000-0002", SANDBOX_ORCID));

            try (var statement = connection.createStatement();
                 var rs = statement.executeQuery("select count(*) from contributor_status_reset where reset_at is null")) {
                rs.next();
                assertThat(rs.getInt(1)).as("nobody is reset yet").isEqualTo(2);
            }
        }
    }

    @Test
    @DisplayName("Running the reset again changes nothing further, and Flyway applies it only once")
    void repeatChangesNothing() throws Exception {
        final var database = freshDatabase("contributor_reset_repeat");
        flyway(database, BEFORE_RESET).migrate();

        try (var connection = connectionTo(database)) {
            ensureSchema(connection, ORCID);
            seedContributor(connection, ORCID, "0000-0001-0000-0001", "AUTHENTICATED");
        }

        flyway(database, RESET).migrate();

        try (var connection = connectionTo(database)) {
            final Map<String, String> retainedBefore = retained(connection);
            final String recordedAt;
            try (var statement = connection.createStatement();
                 var rs = statement.executeQuery("select recorded_at::text from contributor_status_reset")) {
                rs.next();
                recordedAt = rs.getString(1);
            }

            runMigrationSql(connection);

            assertThat(statuses(connection)).containsEntry("0000-0001-0000-0001", "AUTHENTICATED");
            assertThat(retained(connection)).isEqualTo(retainedBefore);
            try (var statement = connection.createStatement();
                 var rs = statement.executeQuery("select recorded_at::text from contributor_status_reset")) {
                rs.next();
                assertThat(rs.getString(1)).as("the original record is kept").isEqualTo(recordedAt);
            }
        }

        assertThat(flyway(database, RESET).migrate().migrationsExecuted)
                .as("Flyway doesn't apply the reset twice")
                .isZero();
    }
}
