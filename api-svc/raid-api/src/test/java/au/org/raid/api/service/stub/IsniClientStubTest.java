package au.org.raid.api.service.stub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static au.org.raid.api.service.stub.InMemoryStubTestData.NONEXISTENT_TEST_ISNI;
import static au.org.raid.api.service.stub.InMemoryStubTestData.SERVER_ERROR_TEST_ISNI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IsniClientStubTest {
    private static final Long NO_DELAY = 0L;

    private final IsniClientStub isniClientStub = new IsniClientStub(NO_DELAY);

    @Test
    @DisplayName("exists should return true for an ISNI that is not the nonexistent sentinel")
    void existsReturnsTrueForOrdinaryIsni() {
        assertThat(isniClientStub.exists("https://isni.org/isni/0000000123456789"), is(true));
    }

    @Test
    @DisplayName("exists should return false for the nonexistent-ISNI sentinel")
    void existsReturnsFalseForNonexistentSentinel() {
        assertThat(isniClientStub.exists(NONEXISTENT_TEST_ISNI), is(false));
    }

    @Test
    @DisplayName("exists should throw for the server-error-ISNI sentinel")
    void existsThrowsForServerErrorSentinel() {
        assertThrows(RuntimeException.class, () -> isniClientStub.exists(SERVER_ERROR_TEST_ISNI));
    }

    @Test
    @DisplayName("getName should return a stub name for an ordinary ISNI")
    void getNameReturnsStubNameForOrdinaryIsni() {
        assertThat(isniClientStub.getName("https://isni.org/isni/0000000123456789"), is("Test User"));
    }

    @Test
    @DisplayName("getName should throw for the nonexistent-ISNI sentinel")
    void getNameThrowsForNonexistentSentinel() {
        assertThrows(RuntimeException.class, () -> isniClientStub.getName(NONEXISTENT_TEST_ISNI));
    }

    @Test
    @DisplayName("a null delay should not throw and should behave as no delay")
    void nullDelayDoesNotThrow() {
        final var stubWithNullDelay = new IsniClientStub(null);

        assertThat(stubWithNullDelay.exists("https://isni.org/isni/0000000123456789"), is(true));
        assertThat(stubWithNullDelay.getName("https://isni.org/isni/0000000123456789"), is("Test User"));
    }
}
