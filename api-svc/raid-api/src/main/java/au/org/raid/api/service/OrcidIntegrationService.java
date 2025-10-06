package au.org.raid.api.service;

import au.org.raid.api.dto.ContributorLookupResponse;
import au.org.raid.api.factory.RaidListenerMessageFactory;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.RaidDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrcidIntegrationService {
    private final OrcidIntegrationClient orcidIntegrationClient;
    private final Executor taskExecutor;
    private final RaidListenerMessageFactory messageFactory;

    public OrcidIntegrationService(final OrcidIntegrationClient orcidIntegrationClient,
                                   @Qualifier("applicationTaskExecutor") final Executor taskExecutor,
                                   final RaidListenerMessageFactory messageFactory) {
        this.orcidIntegrationClient = orcidIntegrationClient;
        this.taskExecutor = taskExecutor;
        this.messageFactory = messageFactory;
    }

    public List<Contributor> setContributorStatus(final List<Contributor> contributors) {
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

    public void updateOrcidRecord(final RaidDto raid) {
        final var futures = raid.getContributor().stream()
                .map(contributor -> {
                    final var message = messageFactory.create(
                            raid.getIdentifier(),
                            contributor,
                            raid.getTitle()
                    );
                    return CompletableFuture
                            .runAsync(() -> orcidIntegrationClient.post(message), taskExecutor)
                            .exceptionally(throwable -> {
                                log.error("Error posting message to ORCID integration for contributor ID: {}",
                                        contributor.getId(), throwable);
                                return null;
                            });
                })
                .toList();

        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}