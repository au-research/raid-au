package au.org.raid.api.validator;

import au.org.raid.api.util.TestConstants;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpatialCoverageValidatorTest {
    private SpatialCoveragePlaceValidator placeValidator;
    @BeforeEach
    void setUp() {
        placeValidator = mock(SpatialCoveragePlaceValidator.class);

    }

    @Test
    @DisplayName("Validation passes with valid spatial coverage")
    void validSpatialCoverage() {
        final var uriValidatorMap = Map.of(
                SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_, (BiFunction<String, String, List<ValidationFailure>>) (s, s2) -> Collections.emptyList()
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);

        final var language = new Language()
                .id(TestConstants.LANGUAGE_ID)
                .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML);

        final var places = List.of(new SpatialCoveragePlace()
                .text("London"));

        final var spatialCoverage = new SpatialCoverage()
                .id("https://www.geonames.org/2643743/london.html")
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_)
                .place(places);

        final var failures = validationService.validate(List.of(spatialCoverage));
        assertThat(failures, empty());
        verify(placeValidator).validate(places, 0);
    }

    @Test
    @DisplayName("Adds uri validation failures")
    void addUriValidationFailures() {
        final var failure = new ValidationFailure()
                .fieldId("field-id")
                .errorType("error-type")
                .message("_message");

        final var uriValidatorMap = Map.of(
                SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_, (BiFunction<String, String, List<ValidationFailure>>) (s, s2) -> List.of(failure)
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);
        final var uri = "https://www.geonames.org/2643743/london.html";

        final var spatialCoverage = new SpatialCoverage()
                .id(uri)
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_);

        final var failures = validationService.validate(List.of(spatialCoverage));
        assertThat(failures, is(List.of(failure)));
    }

    @Test
    @DisplayName("Validation fails with null id")
    void nullId() {
        final var uriValidatorMap = Map.of(
                SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_, (BiFunction<String, String, List<ValidationFailure>>) (s, s2) -> Collections.emptyList()
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);
        final var spatialCoverage = new SpatialCoverage()
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_);

        final var failures = validationService.validate(List.of(spatialCoverage));
        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("spatialCoverage[0].id")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails if id is empty string")
    void emptyId() {
        final var uriValidatorMap = Map.of(
                SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_, (BiFunction<String, String, List<ValidationFailure>>) (s, s2) -> Collections.emptyList()
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);

        final var spatialCoverage = new SpatialCoverage()
                .id("")
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_);

        final var failures = validationService.validate(List.of(spatialCoverage));
        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("spatialCoverage[0].id")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails with null schemaUri")
    void nullSchemeUri() {
        final var uriValidatorMap = Map.of(
                SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_, (BiFunction<String, String, List<ValidationFailure>>) (s, s2) -> Collections.emptyList()
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);

        final var spatialCoverage = new SpatialCoverage()
                .id("https://www.geonames.org/2643743/london.html");

        final var failures = validationService.validate(List.of(spatialCoverage));
        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("spatialCoverage[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }
}