package au.org.raid.api.service;

import au.org.raid.api.exception.RelatedObjectNotFoundException;
import au.org.raid.api.exception.RelatedObjectSchemaNotFoundException;
import au.org.raid.api.exception.RelatedObjectTypeNotFoundException;
import au.org.raid.api.exception.RelatedObjectTypeSchemaNotFoundException;
import au.org.raid.api.factory.RelatedObjectFactory;
import au.org.raid.api.factory.record.RaidRelatedObjectRecordFactory;
import au.org.raid.api.factory.record.RelatedObjectRecordFactory;
import au.org.raid.api.repository.*;
import au.org.raid.db.jooq.tables.records.*;
import au.org.raid.idl.raidv2.model.RelatedObject;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.RelatedObjectType;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatedObjectServiceTest {
    @Mock
    private RelatedObjectTypeSchemaRepository relatedObjectTypeSchemaRepository;
    @Mock
    private RelatedObjectTypeRepository relatedObjectTypeRepository;
    @Mock
    private RelatedObjectSchemaRepository relatedObjectSchemaRepository;
    @Mock
    private RelatedObjectRepository relatedObjectRepository;
    @Mock
    private RaidRelatedObjectRepository raidRelatedObjectRepository;
    @Mock
    private RelatedObjectRecordFactory relatedObjectRecordFactory;
    @Mock
    private RaidRelatedObjectRecordFactory raidRelatedObjectRecordFactory;
    @Mock
    private RaidRelatedObjectCategoryService raidRelatedObjectCategoryService;
    @Mock
    private RelatedObjectTypeService relatedObjectTypeService;
    @Mock
    private RelatedObjectFactory relatedObjectFactory;
    @InjectMocks
    private RelatedObjectService relatedObjectService;

    @Test
    @DisplayName("create() saves related object and dependencies")
    void create() {
        final var handle = "_handle";
        final var uri = "_uri";
        final var id = 234;
        final var schemaUri = "schema-uri";
        final var schemaId = 123;
        final var typeSchemaUri = "type-schema-uri";
        final var typeSchemaId = 345;
        final var typeUri = "type-uri";
        final var typeId = 456;
        final var category = new RelatedObjectCategory();
        final var raidRelatedObjectId = 567;

        final var relatedObject = new RelatedObject()
                .id(uri)
                .schemaUri(schemaUri)
                .type(new RelatedObjectType()
                        .id(typeUri)
                        .schemaUri(typeSchemaUri))
                .category(List.of(category));

        final var relatedObjectSchemaRecord = new RelatedObjectSchemaRecord()
                .setId(schemaId);

        final var relatedObjectRecord = new RelatedObjectRecord();

        final var savedRelatedObjectRecord = new RelatedObjectRecord()
                .setId(id);

        final var relatedObjectTypeSchemaRecord = new RelatedObjectTypeSchemaRecord()
                .setId(typeSchemaId);

        final var relatedObjectTypeRecord = new RelatedObjectTypeRecord()
                .setId(typeId);

        final var raidRelatedObjectRecord = new RaidRelatedObjectRecord();
        final var savedRaidRelatedObjectRecord = new RaidRelatedObjectRecord()
                .setId(raidRelatedObjectId);

        when(relatedObjectSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(relatedObjectSchemaRecord));
        when(relatedObjectRecordFactory.create(uri, schemaId)).thenReturn(relatedObjectRecord);
        when(relatedObjectRepository.findOrCreate(relatedObjectRecord)).thenReturn(savedRelatedObjectRecord);
        when(relatedObjectTypeSchemaRepository.findByUri(typeSchemaUri))
                .thenReturn(Optional.of(relatedObjectTypeSchemaRecord));
        when(relatedObjectTypeRepository.findByUriAndSchemaId(typeUri, typeSchemaId))
                .thenReturn(Optional.of(relatedObjectTypeRecord));
        when(raidRelatedObjectRecordFactory.create(handle, id, typeId)).thenReturn(raidRelatedObjectRecord);
        when(raidRelatedObjectRepository.create(raidRelatedObjectRecord)).thenReturn(savedRaidRelatedObjectRecord);

        relatedObjectService.create(List.of(relatedObject), handle);

        verify(raidRelatedObjectCategoryService).create(category, raidRelatedObjectId);
    }

    @Test
    @DisplayName("create() throws RelatedObjectSchemaNotFoundException")
    void createThrowsRelatedObjectSchemaNotFoundException() {
        final var handle = "_handle";
        final var uri = "_uri";
        final var schemaUri = "schema-uri";
        final var typeSchemaUri = "type-schema-uri";
        final var typeUri = "type-uri";
        final var category = new RelatedObjectCategory();

        final var relatedObject = new RelatedObject()
                .id(uri)
                .schemaUri(schemaUri)
                .type(new RelatedObjectType()
                        .id(typeUri)
                        .schemaUri(typeSchemaUri))
                .category(List.of(category));

        when(relatedObjectSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.empty());

        assertThrows(RelatedObjectSchemaNotFoundException.class,
                () -> relatedObjectService.create(List.of(relatedObject), handle));

        verifyNoInteractions(relatedObjectRecordFactory);
        verifyNoInteractions(relatedObjectRepository);
        verifyNoInteractions(relatedObjectTypeSchemaRepository);
        verifyNoInteractions(relatedObjectTypeRepository);
        verifyNoInteractions(raidRelatedObjectRecordFactory);
        verifyNoInteractions(raidRelatedObjectRepository);
        verifyNoInteractions(raidRelatedObjectCategoryService);
    }

    @Test
    @DisplayName("create() throws RelatedObjectTypeSchemaNotFoundException")
    void createThrowsRelatedObjectTypeSchemaNotFoundException() {
        final var handle = "_handle";
        final var uri = "_uri";
        final var id = 234;
        final var schemaUri = "schema-uri";
        final var schemaId = 123;
        final var typeSchemaUri = "type-schema-uri";
        final var typeUri = "type-uri";
        final var category = new RelatedObjectCategory();

        final var relatedObject = new RelatedObject()
                .id(uri)
                .schemaUri(schemaUri)
                .type(new RelatedObjectType()
                        .id(typeUri)
                        .schemaUri(typeSchemaUri))
                .category(List.of(category));

        final var relatedObjectSchemaRecord = new RelatedObjectSchemaRecord()
                .setId(schemaId);

        final var relatedObjectRecord = new RelatedObjectRecord();

        final var savedRelatedObjectRecord = new RelatedObjectRecord()
                .setId(id);

        when(relatedObjectSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(relatedObjectSchemaRecord));
        when(relatedObjectRecordFactory.create(uri, schemaId)).thenReturn(relatedObjectRecord);
        when(relatedObjectRepository.findOrCreate(relatedObjectRecord)).thenReturn(savedRelatedObjectRecord);
        when(relatedObjectTypeSchemaRepository.findByUri(typeSchemaUri)).thenReturn(Optional.empty());

        assertThrows(RelatedObjectTypeSchemaNotFoundException.class,
                () -> relatedObjectService.create(List.of(relatedObject), handle));

        verifyNoInteractions(relatedObjectTypeRepository);
        verifyNoInteractions(raidRelatedObjectRecordFactory);
        verifyNoInteractions(raidRelatedObjectRepository);
        verifyNoInteractions(raidRelatedObjectCategoryService);
    }

    @Test
    @DisplayName("create() throws RelatedObjectTypeNotFoundException")
    void createThrowsRelatedObjectTypeNotFoundException() {
        final var handle = "_handle";
        final var uri = "_uri";
        final var id = 234;
        final var schemaUri = "schema-uri";
        final var schemaId = 123;
        final var typeSchemaUri = "type-schema-uri";
        final var typeSchemaId = 345;
        final var typeUri = "type-uri";
        final var category = new RelatedObjectCategory();

        final var relatedObject = new RelatedObject()
                .id(uri)
                .schemaUri(schemaUri)
                .type(new RelatedObjectType()
                        .id(typeUri)
                        .schemaUri(typeSchemaUri))
                .category(List.of(category));

        final var relatedObjectSchemaRecord = new RelatedObjectSchemaRecord()
                .setId(schemaId);

        final var relatedObjectRecord = new RelatedObjectRecord();

        final var savedRelatedObjectRecord = new RelatedObjectRecord()
                .setId(id);

        final var relatedObjectTypeSchemaRecord = new RelatedObjectTypeSchemaRecord()
                .setId(typeSchemaId);

        when(relatedObjectSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(relatedObjectSchemaRecord));
        when(relatedObjectRecordFactory.create(uri, schemaId)).thenReturn(relatedObjectRecord);
        when(relatedObjectRepository.findOrCreate(relatedObjectRecord)).thenReturn(savedRelatedObjectRecord);
        when(relatedObjectTypeSchemaRepository.findByUri(typeSchemaUri))
                .thenReturn(Optional.of(relatedObjectTypeSchemaRecord));

        when(relatedObjectTypeRepository.findByUriAndSchemaId(typeUri, typeSchemaId))
                .thenReturn(Optional.empty());

        assertThrows(RelatedObjectTypeNotFoundException.class,
                () -> relatedObjectService.create(List.of(relatedObject), handle));

        verifyNoInteractions(raidRelatedObjectRecordFactory);
        verifyNoInteractions(raidRelatedObjectRepository);
        verifyNoInteractions(raidRelatedObjectCategoryService);
    }

    @Test
    @DisplayName("findAllByHandle() returns all related objects for a given raid")
    void findAllByHandle() {
        final var handle = "_handle";
        final var relatedObjectId = 123;
        final var schemaId = 234;
        final var schemaUri = "schema-uri";
        final var typeId = 345;
        final var id = 456;
        final var uri = "_uri";

        final var raidRelatedObjectRecord = new RaidRelatedObjectRecord()
                .setRelatedObjectId(relatedObjectId)
                .setRelatedObjectTypeId(typeId)
                .setId(id);

        final var relatedObjectRecord = new RelatedObjectRecord()
                .setSchemaId(schemaId)
                .setPid(uri);

        final var relatedObjectSchemaRecord = new RelatedObjectSchemaRecord()
                .setUri(schemaUri);

        final var type = new RelatedObjectType();
        final var categories = List.of(new RelatedObjectCategory());
        final var relatedObject = new RelatedObject();

        when(raidRelatedObjectRepository.findAllByHandle(handle)).thenReturn(List.of(raidRelatedObjectRecord));
        when(relatedObjectRepository.findById(relatedObjectId)).thenReturn(Optional.of(relatedObjectRecord));
        when(relatedObjectSchemaRepository.findById(schemaId)).thenReturn(Optional.of(relatedObjectSchemaRecord));
        when(relatedObjectTypeService.findById(typeId)).thenReturn(type);
        when(raidRelatedObjectCategoryService.findAllByRaidRelatedObjectId(id)).thenReturn(categories);
        when(relatedObjectFactory.create(uri, schemaUri, type, categories)).thenReturn(relatedObject);

        assertThat(relatedObjectService.findAllByHandle(handle), is(List.of(relatedObject)));
    }

    @Test
    @DisplayName("findAllByHandle() throws RelatedObjectNotFoundException")
    void findAllByHandleThrowsRelatedObjectNotFoundException() {
        final var handle = "_handle";
        final var relatedObjectId = 123;
        final var typeId = 345;
        final var id = 456;

        final var raidRelatedObjectRecord = new RaidRelatedObjectRecord()
                .setRelatedObjectId(relatedObjectId)
                .setRelatedObjectTypeId(typeId)
                .setId(id);

        when(raidRelatedObjectRepository.findAllByHandle(handle)).thenReturn(List.of(raidRelatedObjectRecord));
        when(relatedObjectRepository.findById(relatedObjectId)).thenReturn(Optional.empty());

        assertThrows(RelatedObjectNotFoundException.class, () -> relatedObjectService.findAllByHandle(handle));

        verifyNoInteractions(relatedObjectSchemaRepository);
        verifyNoInteractions(relatedObjectTypeService);
        verifyNoInteractions(raidRelatedObjectCategoryService);
        verifyNoInteractions(relatedObjectFactory);
    }

    @Test
    @DisplayName("findAllByHandle() throws RelatedObjectSchemaNotFoundException")
    void findAllByHandleThrowsRelatedObjectSchemaNotFoundException() {
        final var handle = "_handle";
        final var relatedObjectId = 123;
        final var schemaId = 234;
        final var typeId = 345;
        final var id = 456;
        final var uri = "_uri";

        final var raidRelatedObjectRecord = new RaidRelatedObjectRecord()
                .setRelatedObjectId(relatedObjectId)
                .setRelatedObjectTypeId(typeId)
                .setId(id);

        final var relatedObjectRecord = new RelatedObjectRecord()
                .setSchemaId(schemaId)
                .setPid(uri);

        when(raidRelatedObjectRepository.findAllByHandle(handle)).thenReturn(List.of(raidRelatedObjectRecord));
        when(relatedObjectRepository.findById(relatedObjectId)).thenReturn(Optional.of(relatedObjectRecord));
        when(relatedObjectSchemaRepository.findById(schemaId)).thenReturn(Optional.empty());

        assertThrows(RelatedObjectSchemaNotFoundException.class, () -> relatedObjectService.findAllByHandle(handle));

        verifyNoInteractions(relatedObjectTypeService);
        verifyNoInteractions(raidRelatedObjectCategoryService);
        verifyNoInteractions(relatedObjectFactory);
    }

    @Test
    @DisplayName("update() deletes and re-inserts ")
    void update() {
        final var handle = "_handle";
        final var uri = "_uri";
        final var id = 234;
        final var schemaUri = "schema-uri";
        final var schemaId = 123;
        final var typeSchemaUri = "type-schema-uri";
        final var typeSchemaId = 345;
        final var typeUri = "type-uri";
        final var typeId = 456;
        final var category = new RelatedObjectCategory();
        final var raidRelatedObjectId = 567;

        final var relatedObject = new RelatedObject()
                .id(uri)
                .schemaUri(schemaUri)
                .type(new RelatedObjectType()
                        .id(typeUri)
                        .schemaUri(typeSchemaUri))
                .category(List.of(category));

        final var relatedObjectSchemaRecord = new RelatedObjectSchemaRecord()
                .setId(schemaId);

        final var relatedObjectRecord = new RelatedObjectRecord();

        final var savedRelatedObjectRecord = new RelatedObjectRecord()
                .setId(id);

        final var relatedObjectTypeSchemaRecord = new RelatedObjectTypeSchemaRecord()
                .setId(typeSchemaId);

        final var relatedObjectTypeRecord = new RelatedObjectTypeRecord()
                .setId(typeId);

        final var raidRelatedObjectRecord = new RaidRelatedObjectRecord();
        final var savedRaidRelatedObjectRecord = new RaidRelatedObjectRecord()
                .setId(raidRelatedObjectId);

        when(relatedObjectSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(relatedObjectSchemaRecord));
        when(relatedObjectRecordFactory.create(uri, schemaId)).thenReturn(relatedObjectRecord);
        when(relatedObjectRepository.findOrCreate(relatedObjectRecord)).thenReturn(savedRelatedObjectRecord);
        when(relatedObjectTypeSchemaRepository.findByUri(typeSchemaUri))
                .thenReturn(Optional.of(relatedObjectTypeSchemaRecord));
        when(relatedObjectTypeRepository.findByUriAndSchemaId(typeUri, typeSchemaId))
                .thenReturn(Optional.of(relatedObjectTypeRecord));
        when(raidRelatedObjectRecordFactory.create(handle, id, typeId)).thenReturn(raidRelatedObjectRecord);
        when(raidRelatedObjectRepository.create(raidRelatedObjectRecord)).thenReturn(savedRaidRelatedObjectRecord);

        relatedObjectService.update(List.of(relatedObject), handle);

        verify(raidRelatedObjectRepository).deleteAllByHandle(handle);
        verify(raidRelatedObjectCategoryService).create(category, raidRelatedObjectId);
    }
}