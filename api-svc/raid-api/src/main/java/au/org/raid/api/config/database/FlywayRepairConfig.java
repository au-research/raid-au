package au.org.raid.api.config.database;

import au.org.raid.db.repair.RepairLog;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

/**
 * Lets an operator trigger a one-off Flyway repair through reviewed configuration
 * rather than by running SQL against a database.
 *
 * <p>A repair cannot be delivered as a migration. {@code migrate()} validates
 * first, so a checksum mismatch aborts before any migration runs and the repair
 * would never execute in the situation that calls for it. Repair also rewrites
 * {@code flyway_schema_history}, which is the table the surrounding migrate
 * operation is writing to. A flag held only in the database fails for the same
 * reason: nothing could set it without the manual SQL we are trying to avoid.
 *
 * <p>So the trigger is configuration and the disarm is the database. Setting
 * {@code raid.db.repair-token} to a value that has not been seen before runs one
 * repair and records the token. Leaving the value in place cannot repair twice,
 * so deployment configuration does not have to be cleaned up afterwards, and
 * {@link RepairLog} doubles as an audit trail of when each repair ran and what
 * justified it. Use the ticket key as the token.
 *
 * <p>Repairing on every boot was rejected deliberately. Repair rewrites stored
 * checksums to match the classpath, which makes drift disappear rather than
 * reporting it; both the V41 out-of-band application and the 2.9.3 production
 * rollback were caught precisely because Flyway refused to start. Repair also
 * does not help a downgrade, where the database holds migrations newer than the
 * artefact — that needs {@code spring.flyway.ignore-migration-patterns} instead.
 */
@Slf4j
@Configuration
public class FlywayRepairConfig {
    private final String repairToken;

    public FlywayRepairConfig(@Value("${raid.db.repair-token:}") final String repairToken) {
        this.repairToken = repairToken;
    }

    @Bean
    public FlywayMigrationStrategy repairTokenMigrationStrategy() {
        return flyway -> {
            if (repairToken == null || repairToken.isBlank()) {
                flyway.migrate();
                return;
            }

            repairOnce(flyway);
            flyway.migrate();
        };
    }

    private void repairOnce(final Flyway flyway) {
        final var configuration = flyway.getConfiguration();

        if (configuration.getDataSource() == null) {
            throw new IllegalStateException(
                    "raid.db.repair-token is set but Flyway has no data source configured");
        }

        final var repairLog = new RepairLog(
                configuration.getDataSource(),
                configuration.getDefaultSchema(),
                configuration.getTable());

        try {
            if (!repairLog.shouldRepair(repairToken)) {
                log.info("Repair token '{}' has already been applied; skipping repair", repairToken);
                return;
            }

            log.warn("Repair token '{}' has not been applied; running a Flyway repair", repairToken);
            flyway.repair();
            repairLog.record(repairToken);
            log.warn("Flyway repair complete and recorded against token '{}'", repairToken);
        } catch (final SQLException e) {
            // Deliberately fatal. Carrying on would migrate without the repair the
            // operator asked for, leaving the instance in a state nobody chose.
            throw new IllegalStateException(
                    "Could not apply the repair requested by raid.db.repair-token", e);
        }
    }
}
