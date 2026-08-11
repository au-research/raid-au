package au.org.raid.api.service.stub;

import au.org.raid.api.service.rrid.RridService;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.service.stub.InMemoryStubTestData.NONEXISTENT_TEST_RRID;
import static au.org.raid.api.service.stub.InMemoryStubTestData.SERVER_ERROR_TEST_RRID;
import static au.org.raid.api.util.ObjectUtil.areEqual;

@Slf4j
public class RridServiceStub extends RridService {
    private final Long delayMilliseconds;
    public RridServiceStub(final Long delayMilliseconds) {
        super(null);
        this.delayMilliseconds = delayMilliseconds != null ? delayMilliseconds : 0L;
    }

    @Override
    @SneakyThrows
    public List<ValidationFailure> validate(String uri, String fieldId) {
        final var failures = new ArrayList<ValidationFailure>();
        final var regex = getRegex();

        if (!uri.matches(regex)) {
            failures.add(
                    new ValidationFailure()
                            .fieldId(fieldId)
                            .errorType(INVALID_VALUE_TYPE)
                            .message(INVALID_VALUE_MESSAGE + " - should match %s".formatted(regex))
            );
        } else {
            log.debug("delay {}", delayMilliseconds);
            log.debug("simulate RRID validation check");

            final var start = Instant.now();
            Thread.sleep(delayMilliseconds);
            final var end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.info("request to {} took {}.{} seconds", uri, duration.getSeconds(), duration.getNano());

            if (areEqual(uri, NONEXISTENT_TEST_RRID)) {
                failures.add(new ValidationFailure()
                        .fieldId(fieldId)
                        .errorType(INVALID_VALUE_TYPE)
                        .message(URI_DOES_NOT_EXIST)
                );
            } else if (areEqual(uri, SERVER_ERROR_TEST_RRID)) {
                failures.add(new ValidationFailure()
                        .fieldId(fieldId)
                        .errorType(INVALID_VALUE_TYPE)
                        .message(SERVER_ERROR)
                );
            }
        }


        return failures;
    }
}
