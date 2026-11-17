package au.org.raid.api.service;

import au.org.raid.api.config.properties.OrcidIntegrationProperties;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrcidIntegrationServiceTest {

    @Mock
    private OrcidIntegrationClient orcidIntegrationClient;

    private OrcidIntegrationService orcidIntegrationService;

    // Use direct executor for testing to avoid async complexity
    private final Executor directExecutor = Runnable::run;

    @BeforeEach
    void setUp() {
        orcidIntegrationService = createService(true, "http://localhost:1080");
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private OrcidIntegrationService createService(final boolean enabled, final String host) {
        return createService(enabled, host, "api-key");
    }

    private OrcidIntegrationService createService(final boolean enabled, final String host, final String apiKey) {
        final var properties = new OrcidIntegrationProperties();
        properties.setEnabled(enabled);
        properties.setHost(host);
        properties.setApiKey(apiKey);

        return new OrcidIntegrationService(
                orcidIntegrationClient,
                directExecutor,
                properties
        );
    }

    private static RaidDto raidOwnedBy(final long servicePointId) {
        return new RaidDto()
                .identifier(new Id()
                        .id("https://raid.org/10.82841/86bf1b6c")
                        .owner(new Owner().servicePoint(BigDecimal.valueOf(servicePointId))))
                .contributor(List.of(new Contributor().id("orcid1")));
    }

    @Test
    void constructor_shouldDefaultToDisabled_whenPropertiesNotSet() {
        final var service = new OrcidIntegrationService(
                orcidIntegrationClient, directExecutor, new OrcidIntegrationProperties());

        service.notifyRaidChanged(raidOwnedBy(99L));

        verifyNoInteractions(orcidIntegrationClient);
    }

    @Test
    void constructor_shouldRefuseToStart_whenEnabledWithoutHost() {
        final var exception = assertThrows(IllegalStateException.class, () -> createService(true, " "));

        assertThat(exception.getMessage(), containsString("raid.orcid-integration.host"));
    }

    @Test
    void constructor_shouldRefuseToStart_whenEnabledWithoutApiKey() {
        final var exception = assertThrows(IllegalStateException.class,
                () -> createService(true, "http://localhost:1080", " "));

        assertThat(exception.getMessage(), containsString("raid.orcid-integration.api-key"));
    }

    @Test
    void constructor_shouldStart_whenDisabledWithoutHost() {
        createService(false, "");
    }

    @Test
    void constructor_shouldStart_whenDisabledWithoutApiKey() {
        createService(false, "", null);
    }

    @Test
    void notifyRaidChanged_shouldNotCallUpdater_whenDisabled() {
        final var service = createService(false, "");

        service.notifyRaidChanged(raidOwnedBy(99L));

        verifyNoInteractions(orcidIntegrationClient);
    }

    @Test
    void notifyRaidChanged_shouldSendHandle_whenNoTransactionIsActive() {
        orcidIntegrationService.notifyRaidChanged(raidOwnedBy(99L));

        verify(orcidIntegrationClient).notifyRaidChanged("10.82841/86bf1b6c");
    }

    @Test
    void notifyRaidChanged_shouldWaitForCommit_whenTransactionIsActive() {
        TransactionSynchronizationManager.initSynchronization();

        orcidIntegrationService.notifyRaidChanged(raidOwnedBy(99L));

        verifyNoInteractions(orcidIntegrationClient);

        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());

        verify(orcidIntegrationClient).notifyRaidChanged("10.82841/86bf1b6c");
    }

    @Test
    void notifyRaidChanged_shouldNotNotify_whenTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        orcidIntegrationService.notifyRaidChanged(raidOwnedBy(99L));

        // A rollback completes the synchronization without calling afterCommit.
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(orcidIntegrationClient);
    }

    @Test
    void notifyRaidChanged_shouldNotThrow_whenUpdaterFails() {
        doThrow(new RuntimeException("Connection refused"))
                .when(orcidIntegrationClient).notifyRaidChanged("10.82841/86bf1b6c");

        assertDoesNotThrow(() -> orcidIntegrationService.notifyRaidChanged(raidOwnedBy(99L)));
    }
}
