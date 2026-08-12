package au.org.raid.api.service.stub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static au.org.raid.api.service.stub.InMemoryStubTestData.NONEXISTENT_TEST_ORCID;
import static au.org.raid.api.service.stub.InMemoryStubTestData.SERVER_ERROR_TEST_ORCID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrcidClientStubTest {
    private static final Long NO_DELAY = 0L;

    private final OrcidClientStub orcidClientStub = new OrcidClientStub(NO_DELAY);

    @Test
    @DisplayName("exists should return true for an ORCID that is not the nonexistent sentinel")
    void existsReturnsTrueForOrdinaryOrcid() {
        assertThat(orcidClientStub.exists("https://orcid.org/0009-0002-5128-5184"), is(true));
    }

    @Test
    @DisplayName("exists should return false for the nonexistent-ORCID sentinel")
    void existsReturnsFalseForNonexistentSentinel() {
        assertThat(orcidClientStub.exists(NONEXISTENT_TEST_ORCID), is(false));
    }

    @Test
    @DisplayName("exists should throw for the server-error-ORCID sentinel")
    void existsThrowsForServerErrorSentinel() {
        assertThrows(RuntimeException.class, () -> orcidClientStub.exists(SERVER_ERROR_TEST_ORCID));
    }

    @Test
    @DisplayName("getName should return a stub name for an ordinary ORCID")
    void getNameReturnsStubNameForOrdinaryOrcid() {
        assertThat(orcidClientStub.getName("https://orcid.org/0009-0002-5128-5184"), is("Test User"));
    }

    @Test
    @DisplayName("getName should throw for the nonexistent-ORCID sentinel")
    void getNameThrowsForNonexistentSentinel() {
        assertThrows(RuntimeException.class, () -> orcidClientStub.getName(NONEXISTENT_TEST_ORCID));
    }

    @Test
    @DisplayName("a null delay should not throw and should behave as no delay")
    void nullDelayDoesNotThrow() {
        final var stubWithNullDelay = new OrcidClientStub(null);

        assertThat(stubWithNullDelay.exists("https://orcid.org/0009-0002-5128-5184"), is(true));
        assertThat(stubWithNullDelay.getName("https://orcid.org/0009-0002-5128-5184"), is("Test User"));
    }
}
