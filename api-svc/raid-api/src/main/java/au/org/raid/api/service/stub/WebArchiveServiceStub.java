package au.org.raid.api.service.stub;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.api.service.webarchive.WebArchiveService;
import au.org.raid.idl.raidv2.model.UnavailableResolver;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static au.org.raid.api.endpoint.message.ValidationMessage.INVALID_VALUE_TYPE;
import static au.org.raid.api.endpoint.message.ValidationMessage.URI_DOES_NOT_EXIST;
import static au.org.raid.api.service.stub.InMemoryStubTestData.NONEXISTENT_TEST_WEB_ARCHIVE;
import static au.org.raid.api.service.stub.InMemoryStubTestData.SERVER_ERROR_TEST_WEB_ARCHIVE;
import static au.org.raid.api.service.stub.InMemoryStubTestData.VALIDATE_ONCE_TEST_WEB_ARCHIVE_PREFIX;
import static au.org.raid.api.util.ObjectUtil.areEqual;

@Slf4j
public class WebArchiveServiceStub extends WebArchiveService {
    private final Long delayMilliseconds;

    /**
     * Urls under {@link InMemoryStubTestData#VALIDATE_ONCE_TEST_WEB_ARCHIVE_PREFIX} already seen
     * by this stub (RAID-935). Thread-safe because requests validate concurrently.
     */
    private final Set<String> validateOnceSeen = ConcurrentHashMap.newKeySet();

    public WebArchiveServiceStub(final Long delayMilliseconds) {
        super(null, Clock.systemUTC());
        this.delayMilliseconds = delayMilliseconds != null ? delayMilliseconds : 0L;
    }

    @Override
    @SneakyThrows
    public List<ValidationFailure> validate(final String uri, final String fieldId) {
        final var failures = new ArrayList<ValidationFailure>(validateLocally(uri, fieldId));

        if (!failures.isEmpty()) {
            return failures;
        }

        log.debug("delay {}", delayMilliseconds);
        log.debug("simulate Web Archive validation check");

        final var start = Instant.now();
        Thread.sleep(delayMilliseconds);
        final var end = Instant.now();
        final var duration = Duration.between(start, end);
        log.info("request to {} took {}.{} seconds", uri, duration.getSeconds(), duration.getNano());

        if (areEqual(uri, NONEXISTENT_TEST_WEB_ARCHIVE)) {
            failures.add(new ValidationFailure()
                    .fieldId(fieldId)
                    .errorType(INVALID_VALUE_TYPE)
                    .message(URI_DOES_NOT_EXIST));
        } else if (areEqual(uri, SERVER_ERROR_TEST_WEB_ARCHIVE)
                || (uri.startsWith(VALIDATE_ONCE_TEST_WEB_ARCHIVE_PREFIX) && !validateOnceSeen.add(uri))) {
            // "Validate once" sentinel: the first call for a url under the prefix passes, any
            // later call is a 503. It proves a url was NOT sent to the resolver again (RAID-935).
            throw new ResolverUnavailableException(List.of(
                    new UnavailableResolver()
                            .field(fieldId)
                            .value(uri)
                            .resolver(resolverName())
                            .downstreamStatus(503)
                            .downstreamMessage("%s resolve %s -> 503".formatted(resolverName(), uri))
            ));
        }

        return failures;
    }
}
