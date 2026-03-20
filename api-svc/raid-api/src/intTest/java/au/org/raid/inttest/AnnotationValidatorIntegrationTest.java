package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.ValidationFailure;
import au.org.raid.inttest.service.RaidApiValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class AnnotationValidatorIntegrationTest extends AbstractIntegrationTest {


    @Test
    void patternErrorWrongPrefix() {
        createRequest.getOrganisation().get(0).setId("http://example.com");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing title");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("organisation[0].id")
                    .errorType("invalidValue")
                    .message("has invalid/unsupported value - should match ^https://ror\\.org/[0-9a-z]{9}$")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void patternErrorRightPrefix() {
        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            fail("Validation exception thrown");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    void patternErrorNotBlank() {
        createRequest.getTitle().get(0).getLanguage().setId("");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing title");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("title[0].language.id")
                    .errorType("invalidValue")
                    .message("has invalid/unsupported value - must match \"^\\s*\\S.*$\"")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
