package au.org.raid.api.service;

import au.org.raid.api.exception.SubjectTypeNotFoundException;
import au.org.raid.api.exception.SubjectTypeSchemaNotFoundException;
import au.org.raid.api.factory.SubjectFactory;
import au.org.raid.api.factory.record.RaidSubjectKeywordRecordFactory;
import au.org.raid.api.factory.record.RaidSubjectRecordFactory;
import au.org.raid.api.repository.RaidSubjectKeywordRepository;
import au.org.raid.api.repository.RaidSubjectRepository;
import au.org.raid.api.repository.SubjectTypeRepository;
import au.org.raid.api.repository.SubjectTypeSchemaRepository;
import au.org.raid.api.repository.dto.SubjectTypeWithSchema;
import au.org.raid.db.jooq.tables.records.RaidSubjectKeywordRecord;
import au.org.raid.db.jooq.tables.records.RaidSubjectRecord;
import au.org.raid.db.jooq.tables.records.SubjectTypeRecord;
import au.org.raid.db.jooq.tables.records.SubjectTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.Language;
import au.org.raid.idl.raidv2.model.Subject;
import au.org.raid.idl.raidv2.model.SubjectKeyword;
import au.org.raid.idl.raidv2.model.SubjectSchemaURIEnum;
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
class SubjectServiceTest {
    private static final SubjectSchemaURIEnum SCHEMA_URI = SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_316;
    @Mock
    private LanguageService languageService;
    @Mock
    private SubjectTypeRepository subjectTypeRepository;
    @Mock
    private RaidSubjectRepository raidSubjectRepository;
    @Mock
    private RaidSubjectRecordFactory raidSubjectRecordFactory;
    @Mock
    private RaidSubjectKeywordRecordFactory raidSubjectKeywordRecordFactory;
    @Mock
    private RaidSubjectKeywordRepository raidSubjectKeywordRepository;
    @Mock
    private SubjectTypeSchemaRepository subjectTypeSchemaRepository;
    @Mock
    private SubjectKeywordService subjectKeywordService;
    @Mock
    private SubjectFactory subjectFactory;
    @InjectMocks
    private SubjectService subjectService;

    @Test
    @DisplayName("create() saves raid subjects")
    void create() {
        final var handle = "_handle";
        final var id = "_id";
        final var uri = "/" + id;
        final var subjectTypeId = "subject-type-id";
        final var raidSubjectId = 123;
        final var subjectId = 234;
        final var languageId = 234;
        final var text = "_text";

        final var subjectTypeWithSchema = SubjectTypeWithSchema.builder()
                .id(subjectId)
                .subjectTypeId(subjectTypeId)
                .build();

        final var raidSubjectRecord = new RaidSubjectRecord();
        final var saved = new RaidSubjectRecord()
                .setId(raidSubjectId);

        final var language = new Language();

        final var keyword = new SubjectKeyword()
                .language(language)
                .text(text);

        final var subject = new Subject()
                .keyword(List.of(keyword))
                .id(uri)
                .schemaUri(SCHEMA_URI);

        final var raidSubjectKeywordRecord = new RaidSubjectKeywordRecord();

        when(subjectTypeRepository.findBySubjectTypeIdAndSchemaUri(id, SCHEMA_URI.getValue())).thenReturn(Optional.of(subjectTypeWithSchema));

        when(raidSubjectRecordFactory.create(handle, subjectId)).thenReturn(raidSubjectRecord);
        when(raidSubjectRepository.create(raidSubjectRecord)).thenReturn(saved);
        when(languageService.findLanguageId(language)).thenReturn(languageId);
        when(raidSubjectKeywordRecordFactory.create(raidSubjectId, text, languageId))
                .thenReturn(raidSubjectKeywordRecord);

        subjectService.create(List.of(subject), handle);

        verify(raidSubjectKeywordRepository).create(raidSubjectKeywordRecord);
    }

    @Test
    @DisplayName("create() does nothing if list of subjects is null")
    void createWithNullSubjects() {
        final var handle = "_handle";

        subjectService.create(null, handle);

        verifyNoInteractions(subjectTypeRepository);
        verifyNoInteractions(raidSubjectRecordFactory);
        verifyNoInteractions(raidSubjectRepository);
        verifyNoInteractions(languageService);
        verifyNoInteractions(raidSubjectKeywordRecordFactory);
        verifyNoInteractions(raidSubjectKeywordRepository);
    }

    @Test
    @DisplayName("create() throws SubjectTypeNotFoundException")
    void createThrowsSubjectTypeNotFoundException() {
        final var handle = "_handle";
        final var id = "_id";
        final var uri = "/" + id;
        final var subject = new Subject()
                .id(uri)
                .schemaUri(SCHEMA_URI);

        when(subjectTypeRepository.findBySubjectTypeIdAndSchemaUri(id, SCHEMA_URI.getValue())).thenReturn(Optional.empty());

        assertThrows(SubjectTypeNotFoundException.class, () -> subjectService.create(List.of(subject), handle));

        verifyNoInteractions(raidSubjectRecordFactory);
        verifyNoInteractions(raidSubjectRepository);
        verifyNoInteractions(languageService);
        verifyNoInteractions(raidSubjectKeywordRecordFactory);
        verifyNoInteractions(raidSubjectKeywordRepository);
    }

    @Test
    @DisplayName("findAllByHandle() returns list of subjects")
    void findAllByHandle() {
        final var handle = "_handle";
        final var subjectTypeId = "subject-type-id";
        final var schemaId = 123;
        final var schemaUri = "schema-uri";
        final var raidSubjectId = 234;
        final var subjectId = 345;

        final var raidSubjectRecord = new RaidSubjectRecord()
                .setId(raidSubjectId)
                .setSubjectTypeId(subjectId);

        final var subjectTypeWithSchema = SubjectTypeWithSchema.builder()
                .id(subjectId)
                .subjectTypeId(subjectTypeId)
                .schemaId(schemaId)
                .schemaUri(schemaUri)
                .build();

        final var keywords = List.of(new SubjectKeyword());

        final var subject = new Subject();

        when(raidSubjectRepository.findAllByHandle(handle)).thenReturn(List.of(raidSubjectRecord));
        when(subjectTypeRepository.findById(subjectId)).thenReturn(Optional.of(subjectTypeWithSchema));
        when(subjectKeywordService.findAllByRaidSubjectId(raidSubjectId)).thenReturn(keywords);
        when(subjectFactory.create(subjectTypeId, schemaUri, keywords)).thenReturn(subject);

        assertThat(subjectService.findAllByHandle(handle), is(List.of(subject)));
    }

    @Test
    @DisplayName("findAllByHandle() throws SubjectTypeNotFoundException")
    void findAllByHandleThrowsSubjectTypeNotFoundException() {
        final var handle = "_handle";
        final var raidSubjectId = 234;
        final var subjectId = 345;

        final var raidSubjectRecord = new RaidSubjectRecord()
                .setId(raidSubjectId)
                .setSubjectTypeId(subjectId);

        when(raidSubjectRepository.findAllByHandle(handle)).thenReturn(List.of(raidSubjectRecord));
        when(subjectTypeRepository.findById(subjectId)).thenReturn(Optional.empty());

        assertThrows(SubjectTypeNotFoundException.class, () -> subjectService.findAllByHandle(handle));

        verifyNoInteractions(subjectTypeSchemaRepository);
        verifyNoInteractions(subjectKeywordService);
        verifyNoInteractions(subjectFactory);
    }

    @Test
    @DisplayName("update() deletes and re-inserts raid subjects")
    void update() {
        final var handle = "_handle";
        final var id = "_id";
        final var uri = "/" + id;
        final var subjectId = 3;
        final var subjectTypeId = "subject-type-id";
        final var raidSubjectId = 123;
        final var languageId = 234;
        final var text = "_text";


        final var subjectTypeRecord = SubjectTypeWithSchema.builder()
                .id(subjectId)
                .subjectTypeId(subjectTypeId)
                .build();

        final var raidSubjectRecord = new RaidSubjectRecord();
        final var saved = new RaidSubjectRecord()
                .setId(raidSubjectId);

        final var language = new Language();

        final var keyword = new SubjectKeyword()
                .language(language)
                .text(text);

        final var subject = new Subject()
                .keyword(List.of(keyword))
                .id(uri)
                .schemaUri(SCHEMA_URI);

        final var raidSubjectKeywordRecord = new RaidSubjectKeywordRecord();

        when(subjectTypeRepository.findBySubjectTypeIdAndSchemaUri(id, SCHEMA_URI.getValue())).thenReturn(Optional.of(subjectTypeRecord));

        when(raidSubjectRecordFactory.create(handle, subjectId)).thenReturn(raidSubjectRecord);
        when(raidSubjectRepository.create(raidSubjectRecord)).thenReturn(saved);
        when(languageService.findLanguageId(language)).thenReturn(languageId);
        when(raidSubjectKeywordRecordFactory.create(raidSubjectId, text, languageId))
                .thenReturn(raidSubjectKeywordRecord);

        subjectService.update(List.of(subject), handle);

        verify(raidSubjectRepository).deleteAllByHandle(handle);
        verify(raidSubjectKeywordRepository).create(raidSubjectKeywordRecord);
    }
}