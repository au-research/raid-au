package au.org.raid.api.validator;

import au.org.raid.api.util.SchemaValues;
import au.org.raid.api.util.TestConstants;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class DescriptionValidatorTest {
    private static final String DESCRIPTION_VALUE = "Test description";

    @Mock
    private DescriptionTypeValidator typeValidationService;
    @Mock
    private LanguageValidator languageValidator;
    @InjectMocks
    private DescriptionValidator validationService;

    @Test
    @DisplayName("Validation passes with valid description")
    void validDescription() {
        final var type = new DescriptionType()
                .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_318)
                .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320);

        final var language = new Language()
                .id(TestConstants.LANGUAGE_ID)
                .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML);

        final var description = new Description()
                .text(DESCRIPTION_VALUE)
                .type(type)
                .language(language);

        final var failures = validationService.validate(List.of(description));

        assertThat(failures, empty());

        verify(typeValidationService).validate(type, 0);
        verify(languageValidator).validate(language, "description[0]");
    }

    @Test
    @DisplayName("Validation fails with missing primary description")
    void missingPrimaryDescription() {
        final var type = new DescriptionType()
                .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_319)
                .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320);

        final var language = new Language()
                .id(TestConstants.LANGUAGE_ID)
                .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML);

        final var description = new Description()
                .text(DESCRIPTION_VALUE)
                .type(type)
                .language(language);

        final var failures = validationService.validate(List.of(description));

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId("description")
                        .errorType("invalidValue")
                        .message("Descriptions are optional but if specified one must be the primary description.")
        )));

        verify(typeValidationService).validate(type, 0);
        verify(languageValidator).validate(language, "description[0]");
    }

    @Test
    @DisplayName("Validation fails with more than one primary description")
    void multiplePrimaryDescriptions() {
        final var type = new DescriptionType()
                .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_318)
                .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320);

        final var language = new Language()
                .id(TestConstants.LANGUAGE_ID)
                .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML);

        final var description1 = new Description()
                .text(DESCRIPTION_VALUE)
                .type(type)
                .language(language);

        final var failures = validationService.validate(List.of(description1, description1, description1));

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId("description[0].type.id")
                        .errorType("invalidValue")
                        .message("There can only be one primary description. This description conflicts with description[1].type.id, description[2].type.id")
        )));

        verify(typeValidationService).validate(type, 0);
        verify(languageValidator).validate(language, "description[0]");

    }
}