package au.org.raid.api.validator;

import au.org.raid.idl.raidv2.model.Language;
import au.org.raid.idl.raidv2.model.SpatialCoveragePlace;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static au.org.raid.api.util.TestConstants.LANGUAGE_SCHEMA_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpatialCoveragePlaceValidatorTest {
    private static final int SPATIAL_COVERAGE_INDEX = 4;
    @Mock
    private LanguageValidator languageValidator;

    @InjectMocks
    private SpatialCoveragePlaceValidator validator;


    @Test
    @DisplayName("Fails if text is null")
    void nullText() {
        final var places = List.of(new SpatialCoveragePlace()
                .text(null));

        final var result = validator.validate(places, SPATIAL_COVERAGE_INDEX);

        assertThat(result, is(List.of(
                new ValidationFailure()
                        .message("field must be set")
                        .errorType("notSet")
                        .fieldId("spatialCoverage[%d].place[0]".formatted(SPATIAL_COVERAGE_INDEX))
        )));
    }

    @Test
    @DisplayName("Fails if text is empty")
    void emptyText() {
        final var places = List.of(new SpatialCoveragePlace()
                .text(""));

        final var result = validator.validate(places, SPATIAL_COVERAGE_INDEX);

        assertThat(result, is(List.of(
                new ValidationFailure()
                        .message("field must be set")
                        .errorType("notSet")
                        .fieldId("spatialCoverage[%d].place[0]".formatted(SPATIAL_COVERAGE_INDEX))
        )));
    }

    @Test
    @DisplayName("Calls language validator")
    void callsLanguageValidator() {
        final var language = new Language()
                .id("eng")
                .schemaUri(LANGUAGE_SCHEMA_URI);

        final var places = List.of(new SpatialCoveragePlace()
                .text("place")
                .language(language));

        final var result = validator.validate(places, SPATIAL_COVERAGE_INDEX);

        assertThat(result, empty());
        verify(languageValidator).validate(language, "spatialCoverage[%d].place[0]".formatted(SPATIAL_COVERAGE_INDEX));
    }
}