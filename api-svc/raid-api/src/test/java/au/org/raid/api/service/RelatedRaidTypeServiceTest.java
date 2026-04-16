package au.org.raid.api.service;

import au.org.raid.api.exception.RelatedRaidTypeNotFoundException;
import au.org.raid.api.exception.RelatedRaidTypeSchemaNotFoundException;
import au.org.raid.api.factory.RelatedRaidTypeFactory;
import au.org.raid.api.repository.RelatedRaidTypeRepository;
import au.org.raid.api.repository.RelatedRaidTypeSchemaRepository;
import au.org.raid.db.jooq.tables.records.RelatedRaidTypeRecord;
import au.org.raid.db.jooq.tables.records.RelatedRaidTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.RelatedRaidType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatedRaidTypeServiceTest {
    @Mock
    private RelatedRaidTypeSchemaRepository relatedRaidTypeSchemaRepository;
    @Mock
    private RelatedRaidTypeRepository relatedRaidTypeRepository;
    @Mock
    private RelatedRaidTypeFactory relatedRaidTypeFactory;
    @InjectMocks
    private RelatedRaidTypeService relatedRaidTypeService;

    @Test
    @DisplayName("findById() returns related raid type")
    void findById() {
        final var id = 123;
        final var uri = "_uri";
        final var schemaId = 234;
        final var schemaUri = "schema-uri";

        final var relatedRaidTypeRecord = new RelatedRaidTypeRecord()
                .setUri(uri)
                .setSchemaId(schemaId);

        final var relatedRaidTypeSchemaRecord = new RelatedRaidTypeSchemaRecord()
                .setUri(schemaUri);

        final var relatedRaidType = new RelatedRaidType()
                .id(uri);

        when(relatedRaidTypeRepository.findById(id)).thenReturn(Optional.of(relatedRaidTypeRecord));
        when(relatedRaidTypeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(relatedRaidTypeSchemaRecord));
        when(relatedRaidTypeFactory.create(uri, schemaUri)).thenReturn(relatedRaidType);

        assertThat(relatedRaidTypeService.findById(id), is(relatedRaidType));
    }

    @Test
    @DisplayName("findById() throws RelatedRaidTypeNotFoundException")
    void findByIdThrowsRelatedRaidTypeNotFoundException() {
        final var id = 123;

        when(relatedRaidTypeRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(RelatedRaidTypeNotFoundException.class, () -> relatedRaidTypeService.findById(id));

        verifyNoInteractions(relatedRaidTypeSchemaRepository);
        verifyNoInteractions(relatedRaidTypeFactory);
    }

    @Test
    @DisplayName("findById() throws RelatedRaidTypeSchemaNotFoundException")
    void findByIdThrowsRelatedRaidTypeSchemaNotFoundException() {
        final var id = 123;
        final var uri = "_uri";
        final var schemaId = 234;

        final var relatedRaidTypeRecord = new RelatedRaidTypeRecord()
                .setUri(uri)
                .setSchemaId(schemaId);

        when(relatedRaidTypeRepository.findById(id)).thenReturn(Optional.of(relatedRaidTypeRecord));
        when(relatedRaidTypeSchemaRepository.findById(schemaId)).thenReturn(Optional.empty());

        assertThrows(RelatedRaidTypeSchemaNotFoundException.class, () -> relatedRaidTypeService.findById(id));

        verifyNoInteractions(relatedRaidTypeFactory);
    }

    @Test
    @DisplayName("findId() returns id of related raid type")
    void findId() {
        final var uri = "_uri";
        final var schemaUri = "schema-uri";
        final var schemaId = 123;
        final var id = 234;

        final var relatedRaidType = new RelatedRaidType()
                .id(uri)
                .schemaUri(schemaUri);

        final var relatedRaidTypeSchemaRecord = new RelatedRaidTypeSchemaRecord()
                .setId(schemaId);

        final var relatedRaidTypeRecord = new RelatedRaidTypeRecord()
                .setId(id);

        when(relatedRaidTypeSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(relatedRaidTypeSchemaRecord));
        when(relatedRaidTypeRepository.findByUriAndSchemaId(uri, schemaId)).thenReturn(Optional.of(relatedRaidTypeRecord));

        assertThat(relatedRaidTypeService.findId(relatedRaidType), is(id));
    }

    @Test
    @DisplayName("findId() throws RelatedRaidTypeSchemaNotFoundException")
    void findIdThrowsRelatedRaidTypeSchemaNptFoundException() {
        final var uri = "_uri";
        final var schemaUri = "schema-uri";

        final var relatedRaidType = new RelatedRaidType()
                .id(uri)
                .schemaUri(schemaUri);

        when(relatedRaidTypeSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.empty());

        assertThrows(RelatedRaidTypeSchemaNotFoundException.class, () -> relatedRaidTypeService.findId(relatedRaidType));

        verifyNoInteractions(relatedRaidTypeRepository);
    }

    @Test
    @DisplayName("findId() throws RelatedRaidTypeNotFoundException")
    void findIdThrowsRelatedRaidTypeNotFoundException() {
        final var uri = "_uri";
        final var schemaUri = "schema-uri";
        final var schemaId = 123;

        final var relatedRaidType = new RelatedRaidType()
                .id(uri)
                .schemaUri(schemaUri);

        final var relatedRaidTypeSchemaRecord = new RelatedRaidTypeSchemaRecord()
                .setId(schemaId);

        when(relatedRaidTypeSchemaRepository.findByUri(schemaUri)).thenReturn(Optional.of(relatedRaidTypeSchemaRecord));
        when(relatedRaidTypeRepository.findByUriAndSchemaId(uri, schemaId)).thenReturn(Optional.empty());

        assertThrows(RelatedRaidTypeNotFoundException.class, () -> relatedRaidTypeService.findId(relatedRaidType));
    }
}