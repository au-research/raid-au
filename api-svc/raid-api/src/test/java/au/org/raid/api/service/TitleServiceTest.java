package au.org.raid.api.service;

import au.org.raid.api.exception.TitleTypeNotFoundException;
import au.org.raid.api.exception.TitleTypeSchemaNotFoundException;
import au.org.raid.api.factory.TitleFactory;
import au.org.raid.api.factory.record.RaidTitleRecordFactory;
import au.org.raid.api.repository.RaidTitleRepository;
import au.org.raid.api.repository.TitleTypeRepository;
import au.org.raid.api.repository.TitleTypeSchemaRepository;
import au.org.raid.db.jooq.tables.records.RaidTitleRecord;
import au.org.raid.db.jooq.tables.records.TitleTypeRecord;
import au.org.raid.db.jooq.tables.records.TitleTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.Language;
import au.org.raid.idl.raidv2.model.Title;
import au.org.raid.idl.raidv2.model.TitleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TitleServiceTest {
    @Mock
    private RaidTitleRepository raidTitleRepository;
    @Mock
    private TitleTypeRepository titleTypeRepository;
    @Mock
    private TitleTypeSchemaRepository titleTypeSchemaRepository;
    @Mock
    private LanguageService languageService;
    @Mock
    private RaidTitleRecordFactory raidTitleRecordFactory;
    @Mock
    private TitleFactory titleFactory;
    @InjectMocks
    private TitleService titleService;

    @Test
    @DisplayName("findAllByHandle() returns list of titles")
    void findAllByHandle() {
        final var handle = "_handle";
        final var titleTypeId = 123;
        final var titleTypeUri = "title-type-uri";
        final var schemaId = 234;
        final var schemaUri = "schema-uri";
        final var languageId = 345;

        final var title = new Title();

        final var raidTitleRecord = new RaidTitleRecord()
                .setTitleTypeId(titleTypeId)
                .setLanguageId(languageId);

        final var titleTypeRecord = new TitleTypeRecord()
                .setSchemaId(schemaId)
                .setUri(titleTypeUri);

        final var titleTypeSchemaRecord = new TitleTypeSchemaRecord()
                .setUri(schemaUri);

        final var language = new Language();

        when(raidTitleRepository.findAllByHandle(handle)).thenReturn(List.of(raidTitleRecord));
        when(titleTypeRepository.findById(titleTypeId)).thenReturn(Optional.of(titleTypeRecord));
        when(titleTypeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(titleTypeSchemaRecord));
        when(languageService.findById(languageId)).thenReturn(language);
        when(titleFactory.create(raidTitleRecord, titleTypeUri, schemaUri, language)).thenReturn(title);

        assertThat(titleService.findAllByHandle(handle), is(List.of(title)));
    }

    @Test
    @DisplayName("findAllByHandle() throws TitleTypeNotFoundException")
    void findAllByHandleThrowsTitleTypeNotFoundException() {
        final var handle = "_handle";
        final var titleTypeId = 123;
        final var languageId = 345;

        final var raidTitleRecord = new RaidTitleRecord()
                .setTitleTypeId(titleTypeId)
                .setLanguageId(languageId);

        when(raidTitleRepository.findAllByHandle(handle)).thenReturn(List.of(raidTitleRecord));
        when(titleTypeRepository.findById(titleTypeId)).thenReturn(Optional.empty());

        assertThrows(TitleTypeNotFoundException.class, () -> titleService.findAllByHandle(handle));

        verifyNoInteractions(titleTypeSchemaRepository);
        verifyNoInteractions(languageService);
        verifyNoInteractions(titleFactory);
    }

    @Test
    @DisplayName("findAllByHandle() throws TitleTypeSchemaNotFoundException")
    void findAllByHandleThrowsTitleTypeSchemaNotFoundException() {
        final var handle = "_handle";
        final var titleTypeId = 123;
        final var titleTypeUri = "title-type-uri";
        final var schemaId = 234;
        final var languageId = 345;

        final var raidTitleRecord = new RaidTitleRecord()
                .setTitleTypeId(titleTypeId)
                .setLanguageId(languageId);

        final var titleTypeRecord = new TitleTypeRecord()
                .setSchemaId(schemaId)
                .setUri(titleTypeUri);

        when(raidTitleRepository.findAllByHandle(handle)).thenReturn(List.of(raidTitleRecord));
        when(titleTypeRepository.findById(titleTypeId)).thenReturn(Optional.of(titleTypeRecord));
        when(titleTypeSchemaRepository.findById(schemaId)).thenReturn(Optional.empty());

        assertThrows(TitleTypeSchemaNotFoundException.class, () -> titleService.findAllByHandle(handle));

        verifyNoInteractions(languageService);
        verifyNoInteractions(titleFactory);
    }

    @Test
    @DisplayName("create() saves titles for raid")
    void create() {
        final var handle = "_handle";
        final var uri = "_uri";
        final var id = 456;
        final var schemaUri = "schema-uri";
        final var schemaId = 123;
        final var languageId = 234;

        final var language = new Language();

        final var type = new TitleType()
                .schemaUri(schemaUri)
                .id(uri);

        final var title = new Title()
                .type(type)
                .language(language);

        final var titleTypeSchemaRecord = new TitleTypeSchemaRecord()
                .setId(schemaId);

        final var titleTypeRecord = new TitleTypeRecord()
                .setId(id);

        final var raidTitleRecord = new RaidTitleRecord();

        when(titleTypeSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(titleTypeSchemaRecord));
        when(titleTypeRepository.findByUriAndSchemaId(uri, schemaId)).thenReturn(Optional.of(titleTypeRecord));
        when(languageService.findLanguageId(language)).thenReturn(languageId);
        when(raidTitleRecordFactory.create(title, handle, id, languageId)).thenReturn(raidTitleRecord);

        titleService.create(List.of(title), handle);

        verify(raidTitleRepository).create(raidTitleRecord);
    }

    @Test
    @DisplayName("create() throws TitleTypeSchemaNotFoundException")
    void createThrowsTitleTypeSchemaNotFoundException() {
        final var handle = "_handle";
        final var uri = "_uri";
        final var schemaUri = "schema-uri";

        final var language = new Language();

        final var type = new TitleType()
                .schemaUri(schemaUri)
                .id(uri);

        final var title = new Title()
                .type(type)
                .language(language);

        when(titleTypeSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.empty());

        assertThrows(TitleTypeSchemaNotFoundException.class, () -> titleService.create(List.of(title), handle));

        verifyNoInteractions(titleTypeRepository);
        verifyNoInteractions(languageService);
        verifyNoInteractions(raidTitleRecordFactory);
        verifyNoInteractions(raidTitleRepository);
    }

    @Test
    @DisplayName("create() throws TitleTypeNotFoundException")
    void createThrowsTitleTypeNotFoundException() {
        final var handle = "_handle";
        final var uri = "_uri";
        final var schemaUri = "schema-uri";
        final var schemaId = 123;

        final var language = new Language();

        final var type = new TitleType()
                .schemaUri(schemaUri)
                .id(uri);

        final var title = new Title()
                .type(type)
                .language(language);

        final var titleTypeSchemaRecord = new TitleTypeSchemaRecord()
                .setId(schemaId);

        when(titleTypeSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(titleTypeSchemaRecord));
        when(titleTypeRepository.findByUriAndSchemaId(uri, schemaId)).thenReturn(Optional.empty());

        assertThrows(TitleTypeNotFoundException.class, () -> titleService.create(List.of(title), handle));

        verifyNoInteractions(languageService);
        verifyNoInteractions(raidTitleRecordFactory);
        verifyNoInteractions(raidTitleRepository);
    }

    @Test
    @DisplayName("update() deletes and re-inserts raid titles")
    void update() {
        final var handle = "_handle";
        final var uri = "_uri";
        final var id = 456;
        final var schemaUri = "schema-uri";
        final var schemaId = 123;
        final var languageId = 234;

        final var language = new Language();

        final var type = new TitleType()
                .schemaUri(schemaUri)
                .id(uri);

        final var title = new Title()
                .type(type)
                .language(language);

        final var titleTypeSchemaRecord = new TitleTypeSchemaRecord()
                .setId(schemaId);

        final var titleTypeRecord = new TitleTypeRecord()
                .setId(id);

        final var raidTitleRecord = new RaidTitleRecord();

        when(titleTypeSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(titleTypeSchemaRecord));
        when(titleTypeRepository.findByUriAndSchemaId(uri, schemaId)).thenReturn(Optional.of(titleTypeRecord));
        when(languageService.findLanguageId(language)).thenReturn(languageId);
        when(raidTitleRecordFactory.create(title, handle, id, languageId)).thenReturn(raidTitleRecord);

        titleService.update(List.of(title), handle);

        verify(raidTitleRepository).deleteAllByHandle(handle);
        verify(raidTitleRepository).create(raidTitleRecord);
    }
}