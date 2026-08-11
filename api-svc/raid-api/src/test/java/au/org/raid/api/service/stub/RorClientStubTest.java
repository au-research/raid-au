package au.org.raid.api.service.stub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static au.org.raid.api.service.stub.InMemoryStubTestData.NONEXISTENT_TEST_ROR;
import static au.org.raid.api.service.stub.InMemoryStubTestData.SERVER_ERROR_TEST_ROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RorClientStubTest {
    private static final Long NO_DELAY = 0L;

    private final RorClientStub rorClientStub = new RorClientStub(NO_DELAY);

    @Test
    @DisplayName("exists should return true for a ROR that is not the nonexistent sentinel")
    void existsReturnsTrueForOrdinaryRor() {
        assertThat(rorClientStub.exists("https://ror.org/038sjwq14"), is(true));
    }

    @Test
    @DisplayName("exists should return false for the nonexistent-ROR sentinel")
    void existsReturnsFalseForNonexistentSentinel() {
        assertThat(rorClientStub.exists(NONEXISTENT_TEST_ROR), is(false));
    }

    @Test
    @DisplayName("exists should throw for the server-error-ROR sentinel")
    void existsThrowsForServerErrorSentinel() {
        assertThrows(RuntimeException.class, () -> rorClientStub.exists(SERVER_ERROR_TEST_ROR));
    }

    @Test
    @DisplayName("getOrganisationName should return a stub name for an ordinary ROR")
    void getOrganisationNameReturnsStubNameForOrdinaryRor() {
        assertThat(rorClientStub.getOrganisationName("https://ror.org/038sjwq14"), is("Test Organisation"));
    }

    @Test
    @DisplayName("getOrganisationName should throw for the nonexistent-ROR sentinel")
    void getOrganisationNameThrowsForNonexistentSentinel() {
        assertThrows(RuntimeException.class, () -> rorClientStub.getOrganisationName(NONEXISTENT_TEST_ROR));
    }
}
