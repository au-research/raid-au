package au.org.raid.api.service.stub;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static au.org.raid.api.service.stub.InMemoryStubTestData.NONEXISTENT_TEST_WEB_ARCHIVE;
import static au.org.raid.api.service.stub.InMemoryStubTestData.SERVER_ERROR_TEST_WEB_ARCHIVE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebArchiveServiceStubTest {
    private static final String FIELD_ID = "relatedObject[0].id";

    private final WebArchiveServiceStub webArchiveServiceStub = new WebArchiveServiceStub(0L);

    @Test
    @DisplayName("A valid, non-sentinel URL passes")
    void validNonSentinelUrlPasses() {
        final var uri = "https://web.archive.org/web/20220101000000/https://example.com";

        final var failures = webArchiveServiceStub.validate(uri, FIELD_ID);

        assertThat(failures, empty());
    }

    @Test
    @DisplayName("NONEXISTENT_TEST_WEB_ARCHIVE returns URI_DOES_NOT_EXIST")
    void nonexistentSentinelReturnsUriDoesNotExist() {
        final var failures = webArchiveServiceStub.validate(NONEXISTENT_TEST_WEB_ARCHIVE, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("uri not found")
        )));
    }

    @Test
    @DisplayName("SERVER_ERROR_TEST_WEB_ARCHIVE throws ResolverUnavailableException")
    void serverErrorSentinelThrowsResolverUnavailable() {
        final var e = assertThrows(ResolverUnavailableException.class,
                () -> webArchiveServiceStub.validate(SERVER_ERROR_TEST_WEB_ARCHIVE, FIELD_ID));

        assertThat(e.getUnavailableResolvers().get(0).getResolver(), is("Web Archive"));
    }

    @Test
    @DisplayName("A sentinel-shaped but implausible-year URL is rejected by the year check, with no sentinel dispatch")
    void implausibleYearRejectedEvenForSentinelShapedUrl() {
        final var uri = "https://web.archive.org/web/14060101000000/https://nonexistent.example.com";

        final var failures = webArchiveServiceStub.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalidValue")
                        .message("web archive timestamp year 1406 is implausible")
        )));
    }

    @Test
    @DisplayName("A malformed URL fails format validation")
    void malformedUrlFailsFormatValidation() {
        final var uri = "https://web.archive.org/foo/bar";

        final var failures = webArchiveServiceStub.validate(uri, FIELD_ID);

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId(FIELD_ID)
                        .errorType("invalid")
                        .message("Must be a valid Web Archive URL (e.g. https://web.archive.org/web/20220101000000/https://example.com)")
        )));
    }
}
