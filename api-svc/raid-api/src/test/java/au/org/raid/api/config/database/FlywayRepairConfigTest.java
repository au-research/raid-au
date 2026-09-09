package au.org.raid.api.config.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * The normal boot path, where no repair is armed. Behaviour with a token needs a
 * real database and is covered by {@code FlywayRepairIntegrationTest}.
 */
class FlywayRepairConfigTest {

    @Test
    @DisplayName("migrates without repairing when no token is configured")
    void migratesWithoutRepairing() {
        final var flyway = mock(Flyway.class);

        new FlywayRepairConfig(null).repairTokenMigrationStrategy().migrate(flyway);

        verify(flyway).migrate();
        verify(flyway, never()).repair();
        // Nothing else is touched, so a normal boot never reaches for a connection
        // or creates the repair log table.
        verifyNoMoreInteractions(flyway);
    }

    @Test
    @DisplayName("treats a blank token as no token")
    void treatsBlankTokenAsAbsent() {
        final var flyway = mock(Flyway.class);

        new FlywayRepairConfig("   ").repairTokenMigrationStrategy().migrate(flyway);

        verify(flyway).migrate();
        verify(flyway, never()).repair();
        verifyNoMoreInteractions(flyway);
    }
}
