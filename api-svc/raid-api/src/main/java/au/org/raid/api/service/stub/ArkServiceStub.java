package au.org.raid.api.service.stub;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.api.service.ark.ArkService;
import au.org.raid.idl.raidv2.model.UnavailableResolver;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.INVALID_VALUE_TYPE;
import static au.org.raid.api.endpoint.message.ValidationMessage.URI_DOES_NOT_EXIST;
import static au.org.raid.api.service.stub.InMemoryStubTestData.NONEXISTENT_TEST_ARK;
import static au.org.raid.api.service.stub.InMemoryStubTestData.SERVER_ERROR_TEST_ARK;
import static au.org.raid.api.util.ObjectUtil.areEqual;

@Slf4j
public class ArkServiceStub extends ArkService {
    private final Long delayMilliseconds;

    public ArkServiceStub(final Long delayMilliseconds) {
        super(null);
        this.delayMilliseconds = delayMilliseconds != null ? delayMilliseconds : 0L;
    }

    @Override
    @SneakyThrows
    public List<ValidationFailure> validate(final String uri, final String fieldId) {
        final var failures = new ArrayList<ValidationFailure>();

        if (!ARK_URL_PATTERN.matcher(uri).matches()) {
            failures.add(new ValidationFailure()
                    .fieldId(fieldId)
                    .errorType(INVALID_VALUE_TYPE)
                    .message(INVALID_ARK_URL_MESSAGE));
            return failures;
        }

        log.debug("delay {}", delayMilliseconds);
        log.debug("simulate ARK validation check");

        final var start = Instant.now();
        Thread.sleep(delayMilliseconds);
        final var end = Instant.now();
        final var duration = Duration.between(start, end);
        log.info("request to {} took {}.{} seconds", uri, duration.getSeconds(), duration.getNano());

        if (areEqual(uri, NONEXISTENT_TEST_ARK)) {
            failures.add(new ValidationFailure()
                    .fieldId(fieldId)
                    .errorType(INVALID_VALUE_TYPE)
                    .message(URI_DOES_NOT_EXIST));
        } else if (areEqual(uri, SERVER_ERROR_TEST_ARK)) {
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
