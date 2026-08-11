package au.org.raid.api.validator;

import au.org.raid.api.exception.ResolverUnavailableException;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.idl.raidv2.model.SpatialCoverage;
import au.org.raid.idl.raidv2.model.SpatialCoveragePlace;
import au.org.raid.idl.raidv2.model.SpatialCoverageSchemaUriEnum;
import au.org.raid.idl.raidv2.model.UnavailableResolver;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

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
                SchemaValues.GEONAMES_SCHEMA_URI.getUri(), (BiFunction<String, String, List<ValidationFailure>>) (s, s2) -> Collections.emptyList()
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);

        final var places = List.of(new SpatialCoveragePlace()
                .text("London"));

        final var spatialCoverage = new SpatialCoverage()
                .id("https://www.geonames.org/2643743/london.html")
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_)
                .place(places);

        final var failures = validationService.validate(List.of(spatialCoverage)).failures();
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
                SchemaValues.GEONAMES_SCHEMA_URI.getUri(), (BiFunction<String, String, List<ValidationFailure>>) (s, s2) -> List.of(failure)
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);
        final var uri = "https://www.geonames.org/2643743/london.html";

        final var spatialCoverage = new SpatialCoverage()
                .id(uri)
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_);

        final var failures = validationService.validate(List.of(spatialCoverage)).failures();
        assertThat(failures, is(List.of(failure)));
    }

    @Test
    @DisplayName("Validation fails with null id")
    void nullId() {
        final var uriValidatorMap = Map.of(
                SchemaValues.GEONAMES_SCHEMA_URI.getUri(), (BiFunction<String, String, List<ValidationFailure>>) (s, s2) -> Collections.emptyList()
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);
        final var spatialCoverage = new SpatialCoverage()
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_);

        final var failures = validationService.validate(List.of(spatialCoverage)).failures();
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
                SchemaValues.GEONAMES_SCHEMA_URI.getUri(), (BiFunction<String, String, List<ValidationFailure>>) (s, s2) -> Collections.emptyList()
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);

        final var spatialCoverage = new SpatialCoverage()
                .id("")
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_);

        final var failures = validationService.validate(List.of(spatialCoverage)).failures();
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
                SchemaValues.GEONAMES_SCHEMA_URI.getUri(), (BiFunction<String, String, List<ValidationFailure>>) (s, s2) -> Collections.emptyList()
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);

        final var spatialCoverage = new SpatialCoverage()
                .id("https://www.geonames.org/2643743/london.html");

        final var failures = validationService.validate(List.of(spatialCoverage)).failures();
        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("spatialCoverage[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails schemaUri is empty string")
    void emptySchemeUri() {
        final var uriValidatorMap = Map.of(
                SchemaValues.GEONAMES_SCHEMA_URI.getUri(), (BiFunction<String, String, List<ValidationFailure>>) (s, s2) -> Collections.emptyList()
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);
        final var spatialCoverage = new SpatialCoverage()
                .id("https://www.geonames.org/2643743/london.html")
                .schemaUri((SpatialCoverageSchemaUriEnum) null);

        final var failures = validationService.validate(List.of(spatialCoverage)).failures();
        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("spatialCoverage[0].schemaUri")
                        .errorType("notSet")
                        .message("field must be set")
        ));
    }

    @Test
    @DisplayName("Validation fails with invalid schemaUri")
    void invalidSchemeUri() {
        final var uriValidatorMap = Map.of(
                SchemaValues.GEONAMES_SCHEMA_URI.getUri(), (BiFunction<String, String, List<ValidationFailure>>) (s, s2) -> Collections.emptyList()
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);
        final var spatialCoverage = new SpatialCoverage()
                .id("https://www.geonames.org/2643743/london.html")
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_OPENSTREETMAP_ORG_);

        final var failures = validationService.validate(List.of(spatialCoverage)).failures();
        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                new ValidationFailure()
                        .fieldId("spatialCoverage[0].schemaUri")
                        .errorType("invalidValue")
                        .message("schema is unknown/unsupported")
        ));
    }

    @Test
    @DisplayName("A resolver failure for one spatial coverage does not abort validation of the rest of the request")
    void resolverUnavailableForOneSpatialCoverageDoesNotAbortValidationOfOthers() {
        final var geoNamesUri = "https://www.geonames.org/2643743/london.html";
        final var openStreetMapSchemaUri = "https://www.openstreetmap.org/";
        final var openStreetMapUri = "https://www.openstreetmap.org/#map=16/51.5074/-0.1278";

        final var unavailable = new UnavailableResolver()
                .field("spatialCoverage[0].id")
                .value(geoNamesUri)
                .resolver("GeoNames")
                .downstreamStatus(null);

        final var uriValidatorMap = Map.<String, BiFunction<String, String, List<ValidationFailure>>>of(
                SchemaValues.GEONAMES_SCHEMA_URI.getUri(), (id, fieldId) -> {
                    throw new ResolverUnavailableException(List.of(unavailable));
                },
                openStreetMapSchemaUri, (id, fieldId) -> Collections.emptyList()
        );

        final var validationService = new SpatialCoverageValidator(placeValidator, uriValidatorMap);

        final var spatialCoverage1 = new SpatialCoverage()
                .id(geoNamesUri)
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_);

        final var spatialCoverage2 = new SpatialCoverage()
                .id(openStreetMapUri)
                .schemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_OPENSTREETMAP_ORG_);

        final var result = validationService.validate(List.of(spatialCoverage1, spatialCoverage2));

        assertThat(result.failures(), empty());
        assertThat(result.unavailableResolvers(), hasSize(1));
        assertThat(result.unavailableResolvers().get(0), is(unavailable));

        // proves the per-item try/catch didn't abort the loop: the second entry was still
        // checked, and place validation across the full list still ran.
        verify(placeValidator).validate(spatialCoverage1.getPlace(), 0);
        verify(placeValidator).validate(spatialCoverage2.getPlace(), 1);
    }
}