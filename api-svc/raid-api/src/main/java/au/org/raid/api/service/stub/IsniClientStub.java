package au.org.raid.api.service.stub;

import au.org.raid.api.client.contributor.isni.IsniClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

import static au.org.raid.api.service.stub.InMemoryStubTestData.NONEXISTENT_TEST_ISNI;

@Slf4j
public class IsniClientStub extends IsniClient {
    private final Long delayMilliseconds;

    public IsniClientStub(final Long delayMilliseconds) {
        super(null, null);
        this.delayMilliseconds = delayMilliseconds;
    }

    @Override
    @SneakyThrows
    public boolean exists(final String isni) {
        log.debug("delay {}", delayMilliseconds);
        log.debug("simulate ISNI existence check");

        final var start = Instant.now();
        Thread.sleep(delayMilliseconds);
        final var end = Instant.now();
        final var duration = Duration.between(start, end);
        log.info("request to {} took {}.{} seconds", isni, duration.getSeconds(), duration.getNano());

        return !NONEXISTENT_TEST_ISNI.equals(isni);
    }

    @Override
    @SneakyThrows
    public String getName(final String isni) {
        log.debug("delay {}", delayMilliseconds);
        log.debug("simulate ISNI name lookup");

        final var start = Instant.now();
        Thread.sleep(delayMilliseconds);
        final var end = Instant.now();
        final var duration = Duration.between(start, end);
        log.info("request to {} took {}.{} seconds", isni, duration.getSeconds(), duration.getNano());

        return "Test User";
    }
}
