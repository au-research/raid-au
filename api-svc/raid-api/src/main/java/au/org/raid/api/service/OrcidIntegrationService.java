package au.org.raid.api.service;

import au.org.raid.api.config.properties.OrcidIntegrationProperties;
import au.org.raid.idl.raidv2.model.RaidDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Executor;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class OrcidIntegrationService {
    private final OrcidIntegrationClient orcidIntegrationClient;
    private final Executor taskExecutor;
    private final boolean enabled;

    public OrcidIntegrationService(final OrcidIntegrationClient orcidIntegrationClient,
                                   @Qualifier("applicationTaskExecutor") final Executor taskExecutor,
                                   final OrcidIntegrationProperties properties) {
        this.orcidIntegrationClient = orcidIntegrationClient;
        this.taskExecutor = taskExecutor;
        this.enabled = properties.isEnabled();

        if (enabled && isBlank(properties.getHost())) {
            throw new IllegalStateException(
                    "raid.orcid-integration.enabled is true but raid.orcid-integration.host is not set");
        }

        if (enabled && isBlank(properties.getApiKey())) {
            throw new IllegalStateException(
                    "raid.orcid-integration.enabled is true but raid.orcid-integration.api-key is not set");
        }

        if (enabled) {
            log.info("ORCID integration is enabled: host {}", properties.getHost());
        } else {
            log.info("ORCID integration is disabled: the ORCID updater is not told about saved RAiDs");
        }
    }

    /**
     * Tells the ORCID updater that the RAiD was minted or saved, once the transaction commits so the
     * updater reads the saved version. The call runs in the background and a failure is logged, never
     * thrown: the RAiD is saved whether or not the updater hears about it, and the next save tells it
     * again.
     */
    public void notifyRaidChanged(final RaidDto raid) {
        if (!enabled) {
            return;
        }

        final var handle = new Handle(raid.getIdentifier().getId()).toString();
        final Runnable notify = () -> taskExecutor.execute(() -> {
            try {
                orcidIntegrationClient.notifyRaidChanged(handle);
            } catch (final RuntimeException e) {
                log.error("Could not tell the ORCID updater that RAiD {} changed: {}", handle, e.getMessage());
            }
        });

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notify.run();
                }
            });
        } else {
            notify.run();
        }
    }
}
