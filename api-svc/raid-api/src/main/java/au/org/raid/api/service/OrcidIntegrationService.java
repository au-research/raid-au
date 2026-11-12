package au.org.raid.api.service;

import au.org.raid.api.config.properties.OrcidIntegrationProperties;
import au.org.raid.api.dto.ContributorLookupResponse;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.db.jooq.tables.records.ContributorRecord;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.RaidDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class OrcidIntegrationService {
    private static final String UNAUTHENTICATED_STATUS = "UNAUTHENTICATED";

    private final OrcidIntegrationClient orcidIntegrationClient;
    private final Executor taskExecutor;
    private final ContributorRepository contributorRepository;
    private final boolean enabled;

    public OrcidIntegrationService(final OrcidIntegrationClient orcidIntegrationClient,
                                   @Qualifier("applicationTaskExecutor") final Executor taskExecutor,
                                   final ContributorRepository contributorRepository,
                                   final OrcidIntegrationProperties properties) {
        this.orcidIntegrationClient = orcidIntegrationClient;
        this.taskExecutor = taskExecutor;
        this.contributorRepository = contributorRepository;
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
            log.info("ORCID integration is disabled: contributor statuses are not looked up and the ORCID updater is not told about saved RAiDs");
        }
    }

    public List<Contributor> setContributorStatus(final List<Contributor> contributors) {
        if (!enabled) {
            // Keep any status already stored for the contributor: the contributor row is shared across
            // RAiDs, so overwriting it would downgrade a contributor who authenticated earlier.
            contributors.forEach(contributor -> contributor.setStatus(
                    contributorRepository.findByPid(contributor.getId())
                            .map(ContributorRecord::getStatus)
                            .filter(status -> !isBlank(status))
                            .orElse(UNAUTHENTICATED_STATUS)));
            return contributors;
        }

        // Create a map of contributor ID to status lookup futures
        Map<String, CompletableFuture<String>> statusFutureMap = contributors.stream()
                .collect(Collectors.toMap(
                        Contributor::getId,
                        contributor -> CompletableFuture
                                .supplyAsync(() -> lookupStatus(contributor.getId()), taskExecutor)
                                .exceptionally(throwable -> {
                                    log.error("Error looking up contributor status for ID: {}",
                                            contributor.getId(), throwable);
                                    return "AWAITING_AUTHENTICATION";
                                })
                ));

        // Single-threaded: update each contributor with its looked-up status
        contributors.forEach(contributor -> {
            String status = statusFutureMap.get(contributor.getId()).join();
            contributor.setStatus(status);
        });

        return contributors;
    }

    private String lookupStatus(final String orcidId) {
        return this.orcidIntegrationClient.findByOrcid(orcidId)
                .map(ContributorLookupResponse::getStatus)
                .orElse("AWAITING_AUTHENTICATION");
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
