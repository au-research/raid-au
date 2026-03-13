package au.org.raid.api.service;

import au.org.raid.api.exception.LanguageNotFoundException;
import au.org.raid.api.exception.LanguageSchemaNotFoundException;
import au.org.raid.api.factory.LanguageFactory;
import au.org.raid.api.repository.LanguageRepository;
import au.org.raid.api.repository.LanguageSchemaRepository;
import au.org.raid.db.jooq.tables.records.LanguageRecord;
import au.org.raid.db.jooq.tables.records.LanguageSchemaRecord;
import au.org.raid.idl.raidv2.model.Language;
import au.org.raid.idl.raidv2.model.LanguageSchemaURIEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LanguageServiceTest {
    @Mock
    private LanguageRepository languageRepository;
    @Mock
    private LanguageSchemaRepository languageSchemaRepository;
    @Mock
    private LanguageFactory languageFactory;
    @InjectMocks
    private LanguageService languageService;

    @Test
    @DisplayName("findLanguageId() returns id from Language")
    void findLanguageId() {
        final var id = 123;
        final var code = "_code";
        final var schemaId = 234;
        final var schemaUri = LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML;

        final var language = new Language()
                .id(code)
                .schemaUri(schemaUri);

        final var languageSchemaRecord = new LanguageSchemaRecord()
                .setId(schemaId)
                .setUri(schemaUri.getValue());

        final var languageRecord = new LanguageRecord()
                .setId(id);

        when(languageSchemaRepository.findByUri(schemaUri.getValue())).thenReturn(Optional.of(languageSchemaRecord));
        when(languageRepository.findByIdAndSchemaId(code, schemaId)).thenReturn(Optional.of(languageRecord));

        final var result = languageService.findLanguageId(language);

        assertThat(result, is(id));
    }

    @Test
    @DisplayName("findLanguageId() returns null if language is null")
    void findLanguageIdReturnsNull() {
        assertThat(languageService.findLanguageId(null), nullValue());

        verifyNoInteractions(languageSchemaRepository);
        verifyNoInteractions(languageRepository);
    }

    @Test
    @DisplayName("findLanguageId() throws LanguageSchemaNotFoundException")
    void findLanguageIdThrowsLanguageSchemaNotFoundException() {
        final var schemaUri = LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML;

        final var language = new Language()
                .id("eng")
                .schemaUri(schemaUri);

        when(languageSchemaRepository.findByUri(schemaUri.getValue())).thenReturn(Optional.empty());

        assertThrows(LanguageSchemaNotFoundException.class, () -> languageService.findLanguageId(language));

        verifyNoInteractions(languageRepository);
    }

    @Test
    @DisplayName("findLanguageId() throws LanguageNotFoundException")
    void findLanguageIdThrowsLanguageNotFoundException() {
        final var code = "_code";
        final var schemaId = 234;
        final var schemaUri = LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML;

        final var language = new Language()
                .id(code)
                .schemaUri(schemaUri);

        final var languageSchemaRecord = new LanguageSchemaRecord()
                .setId(schemaId)
                .setUri(schemaUri.getValue());

        when(languageSchemaRepository.findByUri(schemaUri.getValue())).thenReturn(Optional.of(languageSchemaRecord));
        when(languageRepository.findByIdAndSchemaId(code, schemaId)).thenReturn(Optional.empty());

        assertThrows(LanguageNotFoundException.class, () -> languageService.findLanguageId(language));
    }

    @Test
    @DisplayName("findById() returns a Language given an id")
    void findByIdReturnsLanguage() {
        final var code = "_code";
        final var id = 123;
        final var schemaId = 234;
        final var schemaUri = "schema-uri";

        final var languageRecord = new LanguageRecord()
                .setCode(code)
                .setSchemaId(schemaId);

        final var languageSchemaRecord = new LanguageSchemaRecord()
                .setUri(schemaUri);

        final var language = new Language();

        when(languageRepository.findById(id)).thenReturn(Optional.of(languageRecord));
        when(languageSchemaRepository.findById(schemaId)).thenReturn(Optional.of(languageSchemaRecord));
        when(languageFactory.create(code, schemaUri)).thenReturn(language);

        final var result = languageService.findById(id);

        assertThat(result, is(language));
    }

    @Test
    @DisplayName("findById() throws LanguageNotFoundException")
    void findByIdReturnsLanguageThrowsLanguageNotFoundException() {
        final var id = 123;

        when(languageRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(LanguageNotFoundException.class, () -> languageService.findById(id));

        verifyNoInteractions(languageSchemaRepository);
        verifyNoInteractions(languageFactory);
    }

    @Test
    @DisplayName("findById() thows LanguageSchemaNotFoundException")
    void findByIdReturnsLanguageThrowsLanguageSchemaNotFoundException() {
        final var code = "_code";
        final var id = 123;
        final var schemaId = 234;

        final var languageRecord = new LanguageRecord()
                .setCode(code)
                .setSchemaId(schemaId);

        when(languageRepository.findById(id)).thenReturn(Optional.of(languageRecord));
        when(languageSchemaRepository.findById(schemaId)).thenReturn(Optional.empty());

        assertThrows(LanguageSchemaNotFoundException.class, () -> languageService.findById(id));
        verifyNoInteractions(languageFactory);
    }

    @Test
    @DisplayName("findLanguageId() returns null when language has null id and null schemaUri")
    void findLanguageIdReturnsNullForEmptyLanguage() {
        final var language = new Language();

        assertThat(languageService.findLanguageId(language), nullValue());

        verifyNoInteractions(languageSchemaRepository);
        verifyNoInteractions(languageRepository);
    }

    @Test
    @DisplayName("findLanguageId() returns null when language has null id and set schemaUri")
    void findLanguageIdReturnsNullForNullIdWithSchemaUri() {
        final var language = new Language()
                .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML);

        assertThat(languageService.findLanguageId(language), nullValue());

        verifyNoInteractions(languageSchemaRepository);
        verifyNoInteractions(languageRepository);
    }
}