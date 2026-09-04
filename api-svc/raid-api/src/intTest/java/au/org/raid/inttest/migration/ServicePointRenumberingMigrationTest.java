package au.org.raid.inttest.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression guard for V46, which renumbers Service Points into the block
 * allocated to the Registration Agency operating the instance.
 *
 * <p>Runs against a throwaway Postgres rather than the shared dev database: the
 * migration rewrites primary keys across four tables and two JSONB documents, so
 * it has to be exercised on a real server and on data it is free to destroy.
 *
 * <p>Distinct from the live rehearsal on RAID-806, which is a one-time manual
 * check against a copy of a real Registration Agency database.
 */
@Testcontainers
class ServicePointRenumberingMigrationTest {
    private static final String SCHEMA = "api_svc";
    private static final long BLOCK_SIZE = 10_000_000L;

    /** The version immediately before the renumbering migration. */
    private static final String BEFORE_RENUMBERING = "45";
    private static final String RENUMBERING = "46";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static Flyway flyway(final String database, final String target, final long start) {
        return Flyway.configure()
                .dataSource(
                        POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + database),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .schemas(SCHEMA)
                .defaultSchema(SCHEMA)
                .createSchemas(true)
                .baselineVersion("25")
                .target(target)
                .placeholders(Map.of(
                        "servicePointIdStart", Long.toString(start),
                        "servicePointIdBlockSize", Long.toString(BLOCK_SIZE)))
                .load();
    }

    /**
     * Each case gets its own database so migration history cannot leak between them.
     */
    private static String freshDatabase(final String name) throws SQLException {
        try (var connection = connection(); var statement = connection.createStatement()) {
            statement.execute("drop database if exists " + name);
            statement.execute("create database " + name);
        }
        return name;
    }

    private static Connection connectionTo(final String database) throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl().replace("/" + POSTGRES.getDatabaseName(), "/" + database),
                POSTGRES.getUsername(),
                POSTGRES.getPassword());
    }

    private static long queryLong(final Connection connection, final String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /**
     * The baseline seeds its own Service Point at 20000000, so tests that need
     * particular ids start from an empty set rather than working around it.
     */
    private static void clearSeededData(final Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("set search_path to " + SCHEMA);
            statement.execute("""
                    truncate table service_point, app_user, user_authz_request, raid, raid_archive cascade
                    """);
        }
    }

    /**
     * A Service Point referenced from every table that carries a service_point_id,
     * plus a stored RAiD document embedding the same value, which is the shape the
     * renumbering has to keep consistent.
     */
    private static void seedServicePoint(final Connection connection, final long id, final String handle)
            throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.execute("set search_path to " + SCHEMA);
            statement.execute("""
                    insert into service_point (id, name, admin_email, tech_email, identifier_owner)
                    overriding system value
                    values (%d, 'Service Point %d', 'admin@example.org', 'tech@example.org',
                            'https://ror.org/038sjwq14')
                    """.formatted(id, id));
            statement.execute("""
                    insert into app_user (service_point_id, email, client_id, subject, id_provider, role)
                    values (%d, 'user%d@example.org', 'client', 'subject-%d', 'GOOGLE', 'SP_USER')
                    """.formatted(id, id, id));
            statement.execute("""
                    insert into raid (handle, service_point_id, url, primary_title, confidential,
                                      metadata_schema, metadata, date_created, start_date)
                    values ('%s', %d, 'https://example.org/%s', 'Title', false,
                            'raido-metadata-schema-v2', '%s'::jsonb, now(), '2020-01-01')
                    """.formatted(handle, id, handle, metadata(id)));
            statement.execute("""
                    insert into raid_archive (handle, service_point_id, primary_title,
                                              metadata_schema, metadata, date_created)
                    values ('%s-archived', %d, 'Archived', 'raido-metadata-schema-v2',
                            '%s'::jsonb, now())
                    """.formatted(handle, id, metadata(id)));
            statement.execute("""
                    insert into user_authz_request (service_point_id, email, client_id, subject,
                                                    id_provider, status, description)
                    values (%d, 'req%d@example.org', 'client', 'subject-req-%d', 'GOOGLE',
                            'REQUESTED', 'fixture')
                    """.formatted(id, id, id));
        }
    }

    private static String metadata(final long servicePoint) {
        return """
                {"identifier": {"owner": {"servicePoint": %d, "id": "https://ror.org/038sjwq14"}}}"""
                .formatted(servicePoint);
    }

    @Test
    @DisplayName("renumbers Service Points and everything that references them into the allocated block")
    void renumbersIntoAllocatedBlock() throws SQLException {
        final var database = freshDatabase("renumber");
        flyway(database, BEFORE_RENUMBERING, 20_000_000L).migrate();

        try (var connection = connectionTo(database)) {
            clearSeededData(connection);
            seedServicePoint(connection, 20_000_000L, "10.1/aaa");
            seedServicePoint(connection, 20_000_002L, "10.1/bbb");

            flyway(database, RENUMBERING, 50_000_000L).migrate();

            try (var statement = connection.createStatement()) {
                statement.execute("set search_path to " + SCHEMA);
            }

            assertThat(queryLong(connection, "select min(id) from service_point")).isEqualTo(50_000_000L);
            // The gap between the seeded ids survives, so ids are not silently compacted.
            assertThat(queryLong(connection, "select max(id) from service_point")).isEqualTo(50_000_002L);

            assertThat(queryLong(connection, "select count(*) from app_user where service_point_id = 50000000"))
                    .isEqualTo(1);
            assertThat(queryLong(connection, "select count(*) from raid where service_point_id = 50000000"))
                    .isEqualTo(1);
            assertThat(queryLong(connection,
                    "select count(*) from user_authz_request where service_point_id = 50000000")).isEqualTo(1);
            // raid_archive has no foreign key, so this proves the explicit update ran.
            assertThat(queryLong(connection, "select count(*) from raid_archive where service_point_id = 50000000"))
                    .isEqualTo(1);

            assertThat(queryLong(connection, """
                    select count(*) from raid
                    where (metadata -> 'identifier' -> 'owner' ->> 'servicePoint')::bigint <> service_point_id
                    """)).isZero();
            assertThat(queryLong(connection, """
                    select count(*) from raid_archive
                    where (metadata -> 'identifier' -> 'owner' ->> 'servicePoint')::bigint <> service_point_id
                    """)).isZero();

            assertThat(queryLong(connection,
                    "select (metadata -> 'identifier' -> 'owner' ->> 'servicePoint')::bigint"
                            + " from raid where handle = '10.1/aaa'")).isEqualTo(50_000_000L);

            // The sequence continues from the renumbered ids rather than colliding.
            assertThat(queryLong(connection, """
                    insert into service_point (name, admin_email, tech_email, identifier_owner)
                    values ('Next', 'a@example.org', 't@example.org', 'https://ror.org/038sjwq14')
                    returning id
                    """)).isEqualTo(50_000_003L);
        }
    }

    @Test
    @DisplayName("changes nothing when Service Points already sit in the allocated block")
    void isNoOpWhenAlreadyInBlock() throws SQLException {
        final var database = freshDatabase("noop");
        flyway(database, BEFORE_RENUMBERING, 30_000_000L).migrate();

        try (var connection = connectionTo(database)) {
            clearSeededData(connection);
            seedServicePoint(connection, 30_000_000L, "10.1/ccc");
            seedServicePoint(connection, 30_000_001L, "10.1/ddd");

            final var transactionBefore = queryLong(connection, "select txid_current()");

            flyway(database, RENUMBERING, 30_000_000L).migrate();

            try (var statement = connection.createStatement()) {
                statement.execute("set search_path to " + SCHEMA);
            }

            assertThat(queryLong(connection, "select min(id) from service_point")).isEqualTo(30_000_000L);
            assertThat(queryLong(connection, "select max(id) from service_point")).isEqualTo(30_000_001L);

            // Nothing was rewritten: every row still carries a transaction id from
            // before the migration ran.
            assertThat(queryLong(connection,
                    "select count(*) from service_point where xmin::text::bigint > " + transactionBefore))
                    .isZero();
            assertThat(queryLong(connection,
                    "select count(*) from raid where xmin::text::bigint > " + transactionBefore)).isZero();
            assertThat(queryLong(connection,
                    "select count(*) from raid_archive where xmin::text::bigint > " + transactionBefore))
                    .isZero();
        }
    }

    @Test
    @DisplayName("seeds a fresh instance's first Service Point at the start of its block")
    void seedsFreshInstanceAtBlockStart() throws SQLException {
        final var database = freshDatabase("fresh");
        flyway(database, BEFORE_RENUMBERING, 60_000_000L).migrate();

        try (var connection = connectionTo(database)) {
            clearSeededData(connection);
        }

        flyway(database, RENUMBERING, 60_000_000L).migrate();

        try (var connection = connectionTo(database)) {
            try (var statement = connection.createStatement()) {
                statement.execute("set search_path to " + SCHEMA);
            }

            assertThat(queryLong(connection, "select count(*) from service_point")).isZero();
            assertThat(queryLong(connection, """
                    insert into service_point (name, admin_email, tech_email, identifier_owner)
                    values ('First', 'a@example.org', 't@example.org', 'https://ror.org/038sjwq14')
                    returning id
                    """)).isEqualTo(60_000_000L);
        }
    }

    @Test
    @DisplayName("refuses to insert a Service Point outside the allocated block")
    void rejectsOutOfBlockInsert() throws SQLException {
        final var database = freshDatabase("bounds");
        flyway(database, RENUMBERING, 40_000_000L).migrate();

        try (var connection = connectionTo(database); var statement = connection.createStatement()) {
            statement.execute("set search_path to " + SCHEMA);

            assertThatThrownBy(() -> statement.execute("""
                    insert into service_point (id, name, admin_email, tech_email, identifier_owner)
                    overriding system value
                    values (99000000, 'Out of block', 'a@example.org', 't@example.org',
                            'https://ror.org/038sjwq14')
                    """))
                    .hasMessageContaining("service_point_id_within_allocated_block");
        }
    }

    @Test
    @DisplayName("rolls back entirely when the data cannot fit the allocated block")
    void rollsBackWhenDataSpansBlocks() throws SQLException {
        final var database = freshDatabase("spans");
        flyway(database, BEFORE_RENUMBERING, 30_000_000L).migrate();

        try (var connection = connectionTo(database)) {
            clearSeededData(connection);
            seedServicePoint(connection, 20_000_000L, "10.1/eee");
            // Far enough above the first that no single offset can bring both inside
            // one block, so the migration must refuse rather than half-apply.
            seedServicePoint(connection, 35_000_000L, "10.1/fff");

            assertThatThrownBy(() -> flyway(database, RENUMBERING, 30_000_000L).migrate())
                    .hasMessageContaining("service_point_id_within_allocated_block");

            try (var statement = connection.createStatement()) {
                statement.execute("set search_path to " + SCHEMA);
            }

            // The database is exactly as it was: no partial renumbering committed.
            assertThat(queryLong(connection, "select min(id) from service_point")).isEqualTo(20_000_000L);
            assertThat(queryLong(connection, "select max(id) from service_point")).isEqualTo(35_000_000L);
            assertThat(queryLong(connection, "select count(*) from raid where service_point_id = 20000000"))
                    .isEqualTo(1);

            // Postgres has transactional DDL, so Flyway rolls back its own history
            // insert along with the migration. Nothing is left half-applied and no
            // failed row blocks the next attempt, so recovering from a bad range
            // does not need a Flyway repair (RAID-864).
            assertThat(queryLong(connection,
                    "select count(*) from " + SCHEMA + ".flyway_schema_history where success = false"))
                    .as("a failed migration must not leave a history row needing repair")
                    .isZero();
            assertThat(queryLong(connection,
                    "select count(*) from " + SCHEMA + ".flyway_schema_history where version = '46'"))
                    .isZero();
        }
    }

    @Test
    @DisplayName("re-runs cleanly once the range is corrected, with no repair needed")
    void recoversWithoutRepair() throws SQLException {
        final var database = freshDatabase("recover");
        flyway(database, BEFORE_RENUMBERING, 30_000_000L).migrate();

        try (var connection = connectionTo(database)) {
            clearSeededData(connection);
            seedServicePoint(connection, 20_000_000L, "10.1/ggg");
            seedServicePoint(connection, 35_000_000L, "10.1/hhh");

            assertThatThrownBy(() -> flyway(database, RENUMBERING, 30_000_000L).migrate())
                    .hasMessageContaining("service_point_id_within_allocated_block");

            // Remove the row that could not fit, exactly as an operator would after
            // reading the failure, then deploy again without any manual repair.
            try (var statement = connection.createStatement()) {
                statement.execute("set search_path to " + SCHEMA);
                statement.execute("delete from raid where service_point_id = 35000000");
                statement.execute("delete from raid_archive where service_point_id = 35000000");
                statement.execute("delete from app_user where service_point_id = 35000000");
                statement.execute("delete from user_authz_request where service_point_id = 35000000");
                statement.execute("delete from service_point where id = 35000000");
            }

            flyway(database, RENUMBERING, 30_000_000L).migrate();

            try (var statement = connection.createStatement()) {
                statement.execute("set search_path to " + SCHEMA);
            }
            assertThat(queryLong(connection, "select min(id) from service_point")).isEqualTo(30_000_000L);
        }
    }
}
