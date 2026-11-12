package au.org.raid.orcidupdater.work;

import au.org.raid.orcidupdater.config.OrcidUpdaterProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Runs the worker on every instance. Turned off in tests that call {@link WorkProcessor} themselves. */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "orcid-updater.work", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class WorkScheduler {
    private final WorkProcessor workProcessor;
    private final OrcidUpdaterProperties properties;

    @Scheduled(fixedDelayString = "${orcid-updater.work.poll-interval:5s}")
    public void poll() {
        try {
            // keeps going while there's a full batch, so a backlog doesn't wait a poll per batch
            while (workProcessor.processDue() >= properties.getWork().getBatchSize()) {
                log.debug("Full batch processed; taking the next");
            }
        } catch (final RuntimeException e) {
            // e.g. the database is down; the next poll tries again
            log.error("Work queue poll failed", e);
        }
    }
}
