package au.org.raid.db.repair;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * Records which Flyway repairs have already been applied, so that arming a repair
 * through configuration fires exactly once.
 *
 * <p>Lives in the db module and uses plain JDBC because it has to run before
 * Flyway does, which rules out both a migration and anything that depends on the
 * schema already existing.
 */
public class RepairLog {
    public static final String TABLE_NAME = "db_repair_log";

    private final DataSource dataSource;
    private final String schema;
    private final String historyTable;

    /**
     * @param schema       the schema to hold the log, or null to use the
     *                     connection's default search path
     * @param historyTable Flyway's schema history table name, used to tell a
     *                     never-migrated database from one that needs repairing
     */
    public RepairLog(final DataSource dataSource, final String schema, final String historyTable) {
        this.dataSource = dataSource;
        this.schema = schema;
        this.historyTable = historyTable;
    }

    /**
     * True when {@code token} has not been recorded, meaning a repair is due.
     *
     * <p>Always false on a database Flyway has never migrated: there is no history
     * to repair. That is not merely an optimisation — creating the log in an empty
     * schema would make it non-empty, and Flyway would then refuse to migrate with
     * "Found non-empty schema(s) but no schema history table", leaving an instance
     * unable to start simply because a repair had been armed.
     */
    public boolean shouldRepair(final String token) throws SQLException {
        try (final var connection = dataSource.getConnection()) {
            if (!hasSchemaHistory(connection)) {
                return false;
            }

            create(connection);

            try (final var statement = connection.prepareStatement(
                    "select 1 from " + table() + " where token = ?")) {
                statement.setString(1, token);
                try (final var results = statement.executeQuery()) {
                    return !results.next();
                }
            }
        }
    }

    private boolean hasSchemaHistory(final Connection connection) throws SQLException {
        final var sql = "select to_regclass(?) is not null";
        final var qualified = schema == null ? historyTable : schema + "." + historyTable;

        try (final var statement = connection.prepareStatement(sql)) {
            statement.setString(1, qualified);
            try (final var results = statement.executeQuery()) {
                return results.next() && results.getBoolean(1);
            }
        }
    }

    public void record(final String token) throws SQLException {
        try (final var connection = dataSource.getConnection()) {
            // Self-sufficient rather than assuming shouldRepair ran first: an
            // implicit ordering contract between the two would be easy to break.
            create(connection);

            try (final var statement = connection.prepareStatement(
                    "insert into " + table() + " (token, repaired_at) values (?, ?)")) {
                statement.setString(1, token);
                statement.setTimestamp(2, Timestamp.from(Instant.now()));
                statement.executeUpdate();
            }
        }
    }

    private String table() {
        return schema == null ? TABLE_NAME : schema + "." + TABLE_NAME;
    }

    /**
     * The schema is created too: a repair can be armed on an instance that has
     * never migrated, where Flyway has not yet created it.
     */
    private void create(final Connection connection) throws SQLException {
        try (final var statement = connection.createStatement()) {
            if (schema != null) {
                statement.execute("create schema if not exists " + schema);
            }
            statement.execute("""
                    create table if not exists %s (
                        token varchar(256) primary key not null,
                        repaired_at timestamp not null
                    )""".formatted(table()));
        }
    }
}
