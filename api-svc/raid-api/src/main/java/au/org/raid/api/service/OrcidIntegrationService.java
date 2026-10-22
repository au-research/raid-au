package au.org.raid.api.service;

import au.org.raid.api.config.properties.OrcidIntegrationProperties;
import au.org.raid.api.dto.ContributorLookupResponse;
import au.org.raid.api.exception.ServicePointNotFoundException;
import au.org.raid.api.factory.RaidListenerMessageFactory;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.db.jooq.tables.records.ContributorRecord;
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

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class OrcidIntegrationService {
    private static final String UNAUTHENTICATED_STATUS = "UNAUTHENTICATED";

    private final OrcidIntegrationClient orcidIntegrationClient;
    private final Executor taskExecutor;
    private final RaidListenerMessageFactory messageFactory;
    private final ServicePointService servicePointService;
    private final ContributorRepository contributorRepository;
    private final boolean enabled;

    public OrcidIntegrationService(final OrcidIntegrationClient orcidIntegrationClient,
                                   @Qualifier("applicationTaskExecutor") final Executor taskExecutor,
                                   final RaidListenerMessageFactory messageFactory, ServicePointService servicePointService,
                                   final ContributorRepository contributorRepository,
                                   final OrcidIntegrationProperties properties) {
        this.orcidIntegrationClient = orcidIntegrationClient;
        this.taskExecutor = taskExecutor;
        this.messageFactory = messageFactory;
        this.servicePointService = servicePointService;
        this.contributorRepository = contributorRepository;
        this.enabled = properties.isEnabled();

        if (enabled && isBlank(properties.getHost())) {
            throw new IllegalStateException(
                    "raid.orcid-integration.enabled is true but raid.orcid-integration.host is not set");
        }

        if (enabled) {
            log.info("ORCID integration is enabled: host {}", properties.getHost());
        } else {
            log.info("ORCID integration is disabled: contributor statuses are not looked up and RAiDs are not sent to an ORCID integration host");
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

    public void updateOrcidRecord(final RaidDto raid) {
        if (!enabled) {
            return;
        }

        final var servicePointId = raid.getIdentifier().getOwner().getServicePoint().longValueExact();

        final var servicePoint = servicePointService.findById(servicePointId)
                .orElseThrow(() -> new ServicePointNotFoundException(servicePointId));

        final var message = messageFactory.create(
                raid, servicePoint
        );

        orcidIntegrationClient.post(message);
    }
}
