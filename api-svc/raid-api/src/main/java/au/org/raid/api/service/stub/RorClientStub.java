package au.org.raid.api.service.stub;

import au.org.raid.api.client.ror.RorClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;

import static au.org.raid.api.service.stub.InMemoryStubTestData.NONEXISTENT_TEST_ROR;
import static au.org.raid.api.service.stub.InMemoryStubTestData.SERVER_ERROR_TEST_ROR;

@Slf4j
public class RorClientStub extends RorClient {
    private final Long delayMilliseconds;

    public RorClientStub(final Long delayMilliseconds) {
        super(null, null);
        this.delayMilliseconds = delayMilliseconds != null ? delayMilliseconds : 0L;
    }

    @Override
    @SneakyThrows
    public boolean exists(final String ror) {
        log.debug("delay {}", delayMilliseconds);
        log.debug("simulate ROR existence check");

        final var start = Instant.now();
        Thread.sleep(delayMilliseconds);
        final var end = Instant.now();
        final var duration = Duration.between(start, end);
        log.info("request to {} took {}.{} seconds", ror, duration.getSeconds(), duration.getNano());

        if (SERVER_ERROR_TEST_ROR.equals(ror)) {
            throw new RuntimeException("Simulated server error for ROR %s".formatted(ror));
        }

        return !NONEXISTENT_TEST_ROR.equals(ror);
    }

    @Override
    @SneakyThrows
    public String getOrganisationName(final String id) {
        log.debug("delay {}", delayMilliseconds);
        log.debug("simulate ROR organisation name lookup");

        final var start = Instant.now();
        Thread.sleep(delayMilliseconds);
        final var end = Instant.now();
        final var duration = Duration.between(start, end);
        log.info("request to {} took {}.{} seconds", id, duration.getSeconds(), duration.getNano());

        if (NONEXISTENT_TEST_ROR.equals(id)) {
            throw new RuntimeException("ROR not found %s".formatted(id));
        }

        return "Test Organisation";
    }
}
