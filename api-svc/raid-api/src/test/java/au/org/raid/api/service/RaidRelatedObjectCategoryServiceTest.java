package au.org.raid.api.service;

import au.org.raid.api.exception.RelatedObjectCategoryNotFoundException;
import au.org.raid.api.exception.RelatedObjectCategorySchemaNotFoundException;
import au.org.raid.api.factory.RelatedObjectCategoryFactory;
import au.org.raid.api.factory.record.RaidRelatedObjectCategoryRecordFactory;
import au.org.raid.api.repository.RaidRelatedObjectCategoryRepository;
import au.org.raid.api.repository.RelatedObjectCategoryRepository;
import au.org.raid.api.repository.RelatedObjectCategorySchemaRepository;
import au.org.raid.db.jooq.tables.records.RaidRelatedObjectCategoryRecord;
import au.org.raid.db.jooq.tables.records.RelatedObjectCategoryRecord;
import au.org.raid.db.jooq.tables.records.RelatedObjectCategorySchemaRecord;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
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
class RaidRelatedObjectCategoryServiceTest {
    @Mock
    private RelatedObjectCategorySchemaRepository relatedObjectCategorySchemaRepository;
    @Mock
    private RelatedObjectCategoryRepository relatedObjectCategoryRepository;
    @Mock
    private RaidRelatedObjectCategoryRepository raidRelatedObjectCategoryRepository;
    @Mock
    private RaidRelatedObjectCategoryRecordFactory raidRelatedObjectCategoryRecordFactory;
    @Mock
    private RelatedObjectCategoryFactory relatedObjectCategoryFactory;
    @InjectMocks
    private RaidRelatedObjectCategoryService raidRelatedObjectCategoryService;
    @Test
    @DisplayName("create() saves related object categories")
    void create() {
        final var uri = "_uri";
        final var id = 123;
        final var schemaUri = "schema-uri";
        final var schemaId = 234;
        final var raidRelatedObjectId = 456;

        final var category = new RelatedObjectCategory()
                .id(uri)
                .schemaUri(schemaUri);

        final var schemaRecord = new RelatedObjectCategorySchemaRecord()
                .setId(schemaId);

        final var categoryRecord = new RelatedObjectCategoryRecord()
                .setId(id);

        final var raidRelatedObjectCategoryRecord = new RaidRelatedObjectCategoryRecord();

        when(relatedObjectCategorySchemaRepository.findByUri(schemaUri))
                .thenReturn(Optional.of(schemaRecord));

        when(relatedObjectCategoryRepository.findByUriAndSchemaId(uri, schemaId))
                .thenReturn(Optional.of(categoryRecord));

        when(raidRelatedObjectCategoryRecordFactory.create(raidRelatedObjectId, id))
                .thenReturn(raidRelatedObjectCategoryRecord);

        raidRelatedObjectCategoryService.create(category, raidRelatedObjectId);

        verify(raidRelatedObjectCategoryRepository).create(raidRelatedObjectCategoryRecord);
    }
    @Test
    @DisplayName("create() throws RelatedObjectCategorySchemaNotFoundException")
    void createThrowsRelatedObjectCategorySchemaNotFoundException() {
        final var uri = "_uri";
        final var schemaUri = "schema-uri";
        final var raidRelatedObjectId = 456;

        final var category = new RelatedObjectCategory()
                .id(uri)
                .schemaUri(schemaUri);

        when(relatedObjectCategorySchemaRepository.findByUri(schemaUri))
                .thenReturn(Optional.empty());

        assertThrows(RelatedObjectCategorySchemaNotFoundException.class,
                () -> raidRelatedObjectCategoryService.create(category, raidRelatedObjectId));

        verifyNoInteractions(relatedObjectCategoryRepository);
        verifyNoInteractions(raidRelatedObjectCategoryRecordFactory);
        verifyNoInteractions(raidRelatedObjectCategoryRepository);
    }
    @Test
    @DisplayName("create() throws RelatedObjectCategoryNotFoundException")
    void createThrowsRelatedObjectCategoryNotFoundException() {
        final var uri = "_uri";
        final var schemaUri = "schema-uri";
        final var schemaId = 234;
        final var raidRelatedObjectId = 456;

        final var category = new RelatedObjectCategory()
                .id(uri)
                .schemaUri(schemaUri);

        final var schemaRecord = new RelatedObjectCategorySchemaRecord()
                .setId(schemaId);

        when(relatedObjectCategorySchemaRepository.findByUri(schemaUri))
                .thenReturn(Optional.of(schemaRecord));

        when(relatedObjectCategoryRepository.findByUriAndSchemaId(uri, schemaId))
                .thenReturn(Optional.empty());

        assertThrows(RelatedObjectCategoryNotFoundException.class,
                () -> raidRelatedObjectCategoryService.create(category, raidRelatedObjectId));

        verifyNoInteractions(raidRelatedObjectCategoryRecordFactory);
        verifyNoInteractions(raidRelatedObjectCategoryRepository);
    }

    @Test
    @DisplayName("findAllByRaidRelatedObjectId() returns list of related objects")
    void findAllByRaidRelatedObjectId() {
        final var raidRelatedObjectId = 123;
        final var categoryId = 234;
        final var schemaId = 345;
        final var uri = "_uri";
        final var schemaUri = "schema-uri";

        final var raidRelatedObjectCategoryRecord = new RaidRelatedObjectCategoryRecord()
                .setRelatedObjectCategoryId(categoryId);

        final var relatedObjectCategoryRecord = new RelatedObjectCategoryRecord()
                .setSchemaId(schemaId)
                .setUri(uri);

        final var relatedObjectCategorySchemaRecord = new RelatedObjectCategorySchemaRecord()
                .setUri(schemaUri);

        final var relatedObjectCategory = new RelatedObjectCategory();

        when(raidRelatedObjectCategoryRepository.findAllByRaidRelatedObjectId(raidRelatedObjectId))
                .thenReturn(List.of(raidRelatedObjectCategoryRecord));
        when(relatedObjectCategoryRepository.findById(categoryId)).thenReturn(Optional.of(relatedObjectCategoryRecord));
        when(relatedObjectCategorySchemaRepository.findById(schemaId))
                .thenReturn(Optional.of(relatedObjectCategorySchemaRecord));
        when(relatedObjectCategoryFactory.create(uri, schemaUri)).thenReturn(relatedObjectCategory);

        assertThat(raidRelatedObjectCategoryService.findAllByRaidRelatedObjectId(raidRelatedObjectId),
                is(List.of(relatedObjectCategory)));
    }

    @Test
    @DisplayName("findAllByRaidRelatedObjectId() throws RelatedObjectCategoryNotFoundException")
    void findAllByRaidRelatedObjectIdThrowsRelatedObjectCategoryNotFoundException() {
        final var raidRelatedObjectId = 123;
        final var categoryId = 234;

        final var raidRelatedObjectCategoryRecord = new RaidRelatedObjectCategoryRecord()
                .setRelatedObjectCategoryId(categoryId);

        when(raidRelatedObjectCategoryRepository.findAllByRaidRelatedObjectId(raidRelatedObjectId))
                .thenReturn(List.of(raidRelatedObjectCategoryRecord));
        when(relatedObjectCategoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(RelatedObjectCategoryNotFoundException.class,
                () -> raidRelatedObjectCategoryService.findAllByRaidRelatedObjectId(raidRelatedObjectId));

        verifyNoInteractions(relatedObjectCategorySchemaRepository);
        verifyNoInteractions(relatedObjectCategoryFactory);
    }
    @Test
    @DisplayName("findAllByRaidRelatedObjectId() throws RelatedObjectCategorySchemaNotFoundException")
    void findAllByRaidRelatedObjectIdThrowsRelatedObjectCategorySchemaNotFoundException() {
        final var raidRelatedObjectId = 123;
        final var categoryId = 234;
        final var schemaId = 345;
        final var uri = "_uri";

        final var raidRelatedObjectCategoryRecord = new RaidRelatedObjectCategoryRecord()
                .setRelatedObjectCategoryId(categoryId);

        final var relatedObjectCategoryRecord = new RelatedObjectCategoryRecord()
                .setSchemaId(schemaId)
                .setUri(uri);

        when(raidRelatedObjectCategoryRepository.findAllByRaidRelatedObjectId(raidRelatedObjectId))
                .thenReturn(List.of(raidRelatedObjectCategoryRecord));
        when(relatedObjectCategoryRepository.findById(categoryId)).thenReturn(Optional.of(relatedObjectCategoryRecord));

        when(relatedObjectCategorySchemaRepository.findById(schemaId))
                .thenReturn(Optional.empty());

        assertThrows(RelatedObjectCategorySchemaNotFoundException.class,
                () -> raidRelatedObjectCategoryService.findAllByRaidRelatedObjectId(raidRelatedObjectId));

        verifyNoInteractions(relatedObjectCategoryFactory);
    }
}