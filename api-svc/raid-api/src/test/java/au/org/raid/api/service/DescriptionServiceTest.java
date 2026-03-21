package au.org.raid.api.service;

import au.org.raid.api.exception.DescriptionTypeNotFoundException;
import au.org.raid.api.exception.DescriptionTypeSchemaNotFoundException;
import au.org.raid.api.factory.DescriptionFactory;
import au.org.raid.api.factory.record.RaidDescriptionRecordFactory;
import au.org.raid.api.repository.DescriptionTypeRepository;
import au.org.raid.api.repository.DescriptionTypeSchemaRepository;
import au.org.raid.api.repository.RaidDescriptionRepository;
import au.org.raid.db.jooq.tables.RaidDescription;
import au.org.raid.db.jooq.tables.records.DescriptionTypeRecord;
import au.org.raid.db.jooq.tables.records.DescriptionTypeSchemaRecord;
import au.org.raid.db.jooq.tables.records.RaidDescriptionRecord;
import au.org.raid.idl.raidv2.model.Description;
import au.org.raid.idl.raidv2.model.DescriptionType;
import au.org.raid.idl.raidv2.model.Language;
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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DescriptionServiceTest {
    @Mock private DescriptionTypeSchemaRepository descriptionTypeSchemaRepository;
    @Mock private DescriptionTypeRepository descriptionTypeRepository;
    @Mock private LanguageService languageService;
    @Mock private RaidDescriptionRecordFactory raidDescriptionRecordFactory;
    @Mock private RaidDescriptionRepository raidDescriptionRepository;
    @Mock
    private DescriptionFactory descriptionFactory;
    @InjectMocks
    private DescriptionService descriptionService;
    @Test
    @DisplayName("create() saves all descriptions of raid")
    void create() {
        final var handle = "_handle";
        final var text = "_text";
        final var descriptionTypeUri = "description-type-id";
        final var descriptionTypeSchemaUri = "description-type-schema-uri";
        final var descriptionTypeSchemaId = 123;
        final var descriptionTypeId = 234;
        final var languageId = 345;
        final var languageCode = "language-code";
        final var languageSchemaUri = "language-schema-uri";

        final var type = new DescriptionType()
                .id(descriptionTypeUri)
                .schemaUri(descriptionTypeSchemaUri);

        final var language = new Language()
                .id(languageCode)
                .schemaUri(languageSchemaUri);

        final var description = new Description()
                .text(text)
                .type(type)
                .language(language);

        final var descriptionTypeSchemaRecord = new DescriptionTypeSchemaRecord()
                .setId(descriptionTypeSchemaId);

        final var descriptionTypeRecord = new DescriptionTypeRecord()
                .setId(descriptionTypeId);

        final var raidDescriptionRecord = new RaidDescriptionRecord();

        when(descriptionTypeSchemaRepository.findByUri(descriptionTypeSchemaUri))
                .thenReturn(Optional.of(descriptionTypeSchemaRecord));

        when(descriptionTypeRepository.findByUriAndSchemaId(descriptionTypeUri, descriptionTypeSchemaId))
                .thenReturn(Optional.of(descriptionTypeRecord));

        when(languageService.findLanguageId(language)).thenReturn(languageId);

        when(raidDescriptionRecordFactory.create(handle, text, descriptionTypeId, languageId))
                .thenReturn(raidDescriptionRecord);

        descriptionService.create(List.of(description), handle);

        verify(raidDescriptionRepository).create(raidDescriptionRecord);
    }

    @Test
    @DisplayName("create() does not save with null descriptions")
    void createWithNullDescriptions() {
        final var handle = "_handle";

        try {
            descriptionService.create(null, handle);
        } catch (Exception e) {
            fail("No exception expected");
        }

        verifyNoInteractions(raidDescriptionRepository);
        verifyNoInteractions(descriptionTypeSchemaRepository);
        verifyNoInteractions(descriptionTypeRepository);
        verifyNoInteractions(languageService);
        verifyNoInteractions(raidDescriptionRecordFactory);
    }

    @Test
    @DisplayName("create() throws DescriptionTypeSchemaNotFoundException")
    void createThrowsDescriptionTypeSchemaNotFoundException() {
        final var handle = "_handle";
        final var text = "_text";
        final var descriptionTypeUri = "description-type-id";
        final var descriptionTypeSchemaUri = "description-type-schema-uri";
        final var languageCode = "language-code";
        final var languageSchemaUri = "language-schema-uri";

        final var type = new DescriptionType()
                .id(descriptionTypeUri)
                .schemaUri(descriptionTypeSchemaUri);

        final var language = new Language()
                .id(languageCode)
                .schemaUri(languageSchemaUri);

        final var description = new Description()
                .text(text)
                .type(type)
                .language(language);

        when(descriptionTypeSchemaRepository.findByUri(descriptionTypeSchemaUri))
                .thenReturn(Optional.empty());

        assertThrows(DescriptionTypeSchemaNotFoundException.class,
                () -> descriptionService.create(List.of(description), handle));

        verifyNoInteractions(descriptionTypeRepository);
        verifyNoInteractions(languageService);
        verifyNoInteractions(raidDescriptionRecordFactory);
        verifyNoInteractions(raidDescriptionRepository);
    }

    @Test
    @DisplayName("create() throws DescriptionTypeNotFoundException")
    void createThrowsDescriptionTypeNotFoundException() {
        final var handle = "_handle";
        final var text = "_text";
        final var descriptionTypeUri = "description-type-id";
        final var descriptionTypeSchemaUri = "description-type-schema-uri";
        final var descriptionTypeSchemaId = 123;
        final var languageCode = "language-code";
        final var languageSchemaUri = "language-schema-uri";

        final var type = new DescriptionType()
                .id(descriptionTypeUri)
                .schemaUri(descriptionTypeSchemaUri);

        final var language = new Language()
                .id(languageCode)
                .schemaUri(languageSchemaUri);

        final var description = new Description()
                .text(text)
                .type(type)
                .language(language);

        final var descriptionTypeSchemaRecord = new DescriptionTypeSchemaRecord()
                .setId(descriptionTypeSchemaId);

        when(descriptionTypeSchemaRepository.findByUri(descriptionTypeSchemaUri))
                .thenReturn(Optional.of(descriptionTypeSchemaRecord));

        when(descriptionTypeRepository.findByUriAndSchemaId(descriptionTypeUri, descriptionTypeSchemaId))
                .thenReturn(Optional.empty());

        assertThrows(DescriptionTypeNotFoundException.class,
                () -> descriptionService.create(List.of(description), handle));

        verifyNoInteractions(languageService);
        verifyNoInteractions(raidDescriptionRecordFactory);
        verifyNoInteractions(raidDescriptionRepository);
    }

    @Test
    @DisplayName("findAllByHandle() returns all descriptions for handle")
    void findAllByHandle() {
        final var handle = "_handle";
        final var typeId = 123;
        final var schemaId = 234;
        final var languageId = 345;
        final var uri = "_uri";
        final var schemaUri = "schema-uri";

        final var raidDescriptionRecord = new RaidDescriptionRecord()
                .setDescriptionTypeId(typeId)
                .setLanguageId(languageId);

        final var descriptionTypeRecord = new DescriptionTypeRecord()
                .setUri(uri)
                .setSchemaId(schemaId);

        final var descriptionTypeSchemaRecord = new DescriptionTypeSchemaRecord()
                .setUri(schemaUri);

        final var language = new Language();

        final var description = new Description();

        when(raidDescriptionRepository.findAllByHandle(handle)).thenReturn(List.of(raidDescriptionRecord));
        when(descriptionTypeRepository.findById(typeId)).thenReturn(Optional.of(descriptionTypeRecord));
        when(descriptionTypeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(descriptionTypeSchemaRecord));
        when(languageService.findById(languageId)).thenReturn(language);

        when(descriptionFactory.create(raidDescriptionRecord, uri, schemaUri, language)).thenReturn(description);

        assertThat(descriptionService.findAllByHandle(handle), is(List.of(description)));
    }

    @Test
    @DisplayName("findAllByHandle() returns empty list")
    void findAllByHandleReturnsEmptyList() {
        final var handle = "_handle";

        when(raidDescriptionRepository.findAllByHandle(handle)).thenReturn(Collections.emptyList());

        assertThat(descriptionService.findAllByHandle(handle), is(Collections.emptyList()));

        verifyNoInteractions(descriptionTypeRepository);
        verifyNoInteractions(descriptionTypeSchemaRepository);
        verifyNoInteractions(languageService);
        verifyNoInteractions(descriptionFactory);
    }

    @Test
    @DisplayName("findAllByHandle() throws DescriptionTypeNotFoundException")
    void findAllByHandleThrowsDescriptionTypeNotFoundException() {
        final var handle = "_handle";
        final var typeId = 123;
        final var languageId = 345;

        final var raidDescriptionRecord = new RaidDescriptionRecord()
                .setDescriptionTypeId(typeId)
                .setLanguageId(languageId);

        when(raidDescriptionRepository.findAllByHandle(handle)).thenReturn(List.of(raidDescriptionRecord));
        when(descriptionTypeRepository.findById(typeId)).thenReturn(Optional.empty());

        assertThrows(DescriptionTypeNotFoundException.class, () -> descriptionService.findAllByHandle(handle));

        verifyNoInteractions(descriptionTypeSchemaRepository);
        verifyNoInteractions(languageService);
        verifyNoInteractions(descriptionFactory);
    }

    @Test
    @DisplayName("findAllByHandle() throws DescriptionTypeSchemaNotFoundException")
    void findAllByHandleThrowsDescriptionTypeSchemaNotFoundException() {
        final var handle = "_handle";
        final var typeId = 123;
        final var schemaId = 234;
        final var languageId = 345;
        final var uri = "_uri";

        final var raidDescriptionRecord = new RaidDescriptionRecord()
                .setDescriptionTypeId(typeId)
                .setLanguageId(languageId);

        final var descriptionTypeRecord = new DescriptionTypeRecord()
                .setUri(uri)
                .setSchemaId(schemaId);

        when(raidDescriptionRepository.findAllByHandle(handle)).thenReturn(List.of(raidDescriptionRecord));
        when(descriptionTypeRepository.findById(typeId)).thenReturn(Optional.of(descriptionTypeRecord));
        when(descriptionTypeSchemaRepository.findById(schemaId)).thenReturn(Optional.empty());

        assertThrows(DescriptionTypeSchemaNotFoundException.class, () -> descriptionService.findAllByHandle(handle));

        verifyNoInteractions(languageService);
        verifyNoInteractions(descriptionFactory);
    }

    @Test
    @DisplayName("update() deletes all raid descriptions and re-inserts")
    void update() {
        final var handle = "_handle";
        final var text = "_text";
        final var descriptionTypeUri = "description-type-id";
        final var descriptionTypeSchemaUri = "description-type-schema-uri";
        final var descriptionTypeSchemaId = 123;
        final var descriptionTypeId = 234;
        final var languageId = 345;
        final var languageCode = "language-code";
        final var languageSchemaUri = "language-schema-uri";

        final var type = new DescriptionType()
                .id(descriptionTypeUri)
                .schemaUri(descriptionTypeSchemaUri);

        final var language = new Language()
                .id(languageCode)
                .schemaUri(languageSchemaUri);

        final var description = new Description()
                .text(text)
                .type(type)
                .language(language);

        final var descriptionTypeSchemaRecord = new DescriptionTypeSchemaRecord()
                .setId(descriptionTypeSchemaId);

        final var descriptionTypeRecord = new DescriptionTypeRecord()
                .setId(descriptionTypeId);

        final var raidDescriptionRecord = new RaidDescriptionRecord();

        when(descriptionTypeSchemaRepository.findByUri(descriptionTypeSchemaUri))
                .thenReturn(Optional.of(descriptionTypeSchemaRecord));

        when(descriptionTypeRepository.findByUriAndSchemaId(descriptionTypeUri, descriptionTypeSchemaId))
                .thenReturn(Optional.of(descriptionTypeRecord));

        when(languageService.findLanguageId(language)).thenReturn(languageId);

        when(raidDescriptionRecordFactory.create(handle, text, descriptionTypeId, languageId))
                .thenReturn(raidDescriptionRecord);

        descriptionService.update(List.of(description), handle);

        verify(raidDescriptionRepository).deleteAllByHandle(handle);
        verify(raidDescriptionRepository).create(raidDescriptionRecord);
    }
}