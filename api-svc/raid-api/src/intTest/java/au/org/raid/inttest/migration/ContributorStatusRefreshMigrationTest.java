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
 * Regression guard for V51, which flags every RAiD naming an ORCID contributor so the
 * contributor status refresh worker brings it up to date.
 */
@Testcontainers
class ContributorStatusRefreshMigrationTest {
    private static final String SCHEMA = "api_svc";
    private static final long SERVICE_POINT = 20_000_000L;

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    private static String urlFor(final String database) {
        return POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + database);
    }

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
                        "servicePointIdStart", Long.toString(SERVICE_POINT),
                        "servicePointIdBlockSize", "10000000"))
                .load();
    }

    private static Connection connectionTo(final String database) throws SQLException {
        final var connection = DriverManager.getConnection(
                urlFor(database), POSTGRES.getUsername(), POSTGRES.getPassword());
        try (var statement = connection.createStatement()) {
            statement.execute("set search_path to " + SCHEMA);
        }
        return connection;
    }

    private static void freshDatabase(final String name) throws SQLException {
        try (var connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("drop database if exists " + name);
            statement.execute("create database " + name);
        }
    }

    private static void execute(final Connection connection, final String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void seedRaid(final Connection connection, final String handle) throws SQLException {
        execute(connection, """
                insert into raid (handle, service_point_id, url, primary_title, confidential,
                                  metadata_schema, metadata, date_created, start_date)
                values ('%s', (select min(id) from service_point), 'https://example.org/%s', 'Title', false,
                        'raido-metadata-schema-v2', '{}'::jsonb, now(), '2020-01-01')
                """.formatted(handle, handle));
    }

    private static void addContributor(final Connection connection, final String handle, final String schemaUri,
                                       final String pid) throws SQLException {
        execute(connection, """
                insert into contributor (pid, schema_id, status)
                values ('%s', (select id from contributor_schema where uri = '%s'), 'UNAUTHENTICATED')
                on conflict (pid, schema_id) do nothing
                """.formatted(pid, schemaUri));
        execute(connection, """
                insert into raid_contributor (handle, contributor_id)
                values ('%s', (select c.id from contributor c join contributor_schema s on s.id = c.schema_id
                               where c.pid = '%s' and s.uri = '%s'))
                """.formatted(handle, pid, schemaUri));
    }

    private static Map<String, Boolean> flags(final Connection connection) throws SQLException {
        final var flags = new HashMap<String, Boolean>();
        try (var statement = connection.createStatement();
             var rs = statement.executeQuery("select handle, contributor_status_refresh_required from raid")) {
            while (rs.next()) {
                flags.put(rs.getString(1), rs.getBoolean(2));
            }
        }
        return flags;
    }

    @Test
    @DisplayName("Flags every RAiD naming an ORCID contributor, and no other")
    void flagsRaidsWithOrcidContributors() throws SQLException {
        final var database = "contributor_refresh";
        freshDatabase(database);
        flyway(database, "50").migrate();

        try (var connection = connectionTo(database)) {
            execute(connection, """
                    insert into contributor_schema (uri)
                    select 'https://sandbox.orcid.org/'
                    where not exists (select 1 from contributor_schema where uri = 'https://sandbox.orcid.org/')
                    """);

            seedRaid(connection, "10.1/orcid");
            addContributor(connection, "10.1/orcid", "https://orcid.org/", "0000-0001-0000-0001");

            seedRaid(connection, "10.1/sandbox");
            addContributor(connection, "10.1/sandbox", "https://sandbox.orcid.org/", "0000-0001-0000-0002");

            seedRaid(connection, "10.1/mixed");
            addContributor(connection, "10.1/mixed", "https://isni.org/", "0000000121032683");
            addContributor(connection, "10.1/mixed", "https://orcid.org/", "0000-0001-0000-0001");

            seedRaid(connection, "10.1/isni");
            addContributor(connection, "10.1/isni", "https://isni.org/", "0000000121032683");

            seedRaid(connection, "10.1/none");
        }

        flyway(database, "51").migrate();

        try (var connection = connectionTo(database)) {
            assertThat(flags(connection)).containsOnly(
                    Map.entry("10.1/orcid", true),
                    Map.entry("10.1/sandbox", true),
                    Map.entry("10.1/mixed", true),
                    Map.entry("10.1/isni", false),
                    Map.entry("10.1/none", false));

            seedRaid(connection, "10.1/new");
            assertThat(flags(connection))
                    .as("a RAiD saved later isn't flagged")
                    .containsEntry("10.1/new", false);
        }
    }
}
