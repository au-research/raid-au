package au.org.raid.api.validator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class IsniValidatorTest {
    private IsniValidator validator = new IsniValidator();

    @Test
    void failsWithInvalidIsni() {
        final var invalidIsni = List.of(
                "https://isni.org/1234567890",
                "https://isni.org/abcdefghijklmno",
                "https://isni.org/1234567890123456"
        );

        invalidIsni.forEach(isni -> assertThat(validator.validate(isni), is(false)));
    }

    @Test
    void passesWithValidIsni() {
        final var invalidIsni = List.of(
                "https://isni.org/0000000121032683",  // Valid ISNI
                "https://isni.org/0000000368338633",  // Valid ISNI (check digit is 3)
                "https://isni.org/000000000000001X"   // Valid ISNI with X check digit

        );

        invalidIsni.forEach(isni -> assertThat("%s is not a valid isni".formatted(isni), validator.validate(isni), is(true)));
    }
}