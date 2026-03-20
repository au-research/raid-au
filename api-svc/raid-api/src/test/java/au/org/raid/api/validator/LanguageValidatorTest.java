package au.org.raid.api.validator;

import au.org.raid.api.repository.LanguageRepository;
import au.org.raid.api.repository.LanguageSchemaRepository;
import au.org.raid.db.jooq.tables.records.LanguageRecord;
import au.org.raid.db.jooq.tables.records.LanguageSchemaRecord;
import au.org.raid.idl.raidv2.model.Language;
import au.org.raid.idl.raidv2.model.LanguageSchemaURIEnum;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LanguageValidatorTest {
    private static final String LANGUAGE_ID = "eng";
    private static final int LANGUAGE_SCHEMA_ID = 1;
    private static final String LANGUAGE_SCHEMA_URI = LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML.getValue();

    private static final LanguageRecord LANGUAGE_RECORD = new LanguageRecord()
            .setCode(LANGUAGE_ID)
            .setSchemaId(LANGUAGE_SCHEMA_ID);

    private static final LanguageSchemaRecord LANGUAGE_SCHEMA_RECORD = new LanguageSchemaRecord()
            .setId(LANGUAGE_SCHEMA_ID)
            .setUri(LANGUAGE_SCHEMA_URI);

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private LanguageSchemaRepository languageSchemaRepository;

    @InjectMocks
    private LanguageValidator languageValidator;

    @Test
    @DisplayName("Returns failure if id is not found with schema")
    void invalidId() {
        final var language = new Language()
                .id(LANGUAGE_ID)
                .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML);

        final var languageSchema = new LanguageSchemaRecord().setId(LANGUAGE_SCHEMA_ID);

        when(languageSchemaRepository.findActiveByUri(LANGUAGE_SCHEMA_URI)).thenReturn(Optional.of(languageSchema));
        when(languageRepository.findByIdAndSchemaId(LANGUAGE_ID, LANGUAGE_SCHEMA_ID)).thenReturn(Optional.empty());

        final var failures = languageValidator.validate(language, "parent");

        assertThat(failures, is(List.of(
                new ValidationFailure()
                        .fieldId("parent.language.id")
                        .errorType("invalidValue")
                        .message("id does not exist within the given schema")
        )));
    }

    @Test
    @DisplayName("Valid language returns no failures")
    void validLanguage() {
        final var language = new Language()
                .id(LANGUAGE_ID)
                .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML);

        when(languageSchemaRepository.findActiveByUri(LANGUAGE_SCHEMA_URI)).thenReturn(Optional.of(LANGUAGE_SCHEMA_RECORD));
        when(languageRepository.findByIdAndSchemaId(LANGUAGE_ID, LANGUAGE_SCHEMA_ID))
                .thenReturn(Optional.of(LANGUAGE_RECORD));

        final var failures = languageValidator.validate(language, "parent");

        assertThat(failures, empty());
    }
}