package au.org.raid.api.service.stub;

import au.org.raid.api.client.contributor.orcid.OrcidClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

import static au.org.raid.api.service.stub.InMemoryStubTestData.NONEXISTENT_TEST_ORCID;
import static au.org.raid.api.service.stub.InMemoryStubTestData.SERVER_ERROR_TEST_ORCID;

@Slf4j
public class OrcidClientStub extends OrcidClient {
    private final Long delayMilliseconds;

    public OrcidClientStub(final Long delayMilliseconds) {
        super(null, null);
        this.delayMilliseconds = delayMilliseconds != null ? delayMilliseconds : 0L;
    }

    @Override
    @SneakyThrows
    public boolean exists(final String orcid) {
        log.debug("delay {}", delayMilliseconds);
        log.debug("simulate ORCID existence check");

        final var start = Instant.now();
        Thread.sleep(delayMilliseconds);
        final var end = Instant.now();
        final var duration = Duration.between(start, end);
        log.info("request to {} took {}.{} seconds", orcid, duration.getSeconds(), duration.getNano());

        if (SERVER_ERROR_TEST_ORCID.equals(orcid)) {
            throw new RuntimeException("Simulated server error for ORCID %s".formatted(orcid));
        }

        return !NONEXISTENT_TEST_ORCID.equals(orcid);
    }

    @Override
    @SneakyThrows
    public String getName(final String orcid) {
        log.debug("delay {}", delayMilliseconds);
        log.debug("simulate ORCID name lookup");

        final var start = Instant.now();
        Thread.sleep(delayMilliseconds);
        final var end = Instant.now();
        final var duration = Duration.between(start, end);
        log.info("request to {} took {}.{} seconds", orcid, duration.getSeconds(), duration.getNano());

        if (NONEXISTENT_TEST_ORCID.equals(orcid)) {
            throw new RuntimeException("ORCID not found %s".formatted(orcid));
        }

        return "Test User";
    }
}
