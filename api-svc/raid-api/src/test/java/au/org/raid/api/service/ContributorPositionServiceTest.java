package au.org.raid.api.service;

import au.org.raid.api.exception.ContributorPositionNotFoundException;
import au.org.raid.api.exception.ContributorPositionSchemaNotFoundException;
import au.org.raid.api.factory.ContributorPositionFactory;
import au.org.raid.api.factory.record.RaidContributorPositionRecordFactory;
import au.org.raid.api.repository.ContributorPositionRepository;
import au.org.raid.api.repository.ContributorPositionSchemaRepository;
import au.org.raid.api.repository.RaidContributorPositionRepository;
import au.org.raid.db.jooq.tables.records.ContributorPositionRecord;
import au.org.raid.db.jooq.tables.records.ContributorPositionSchemaRecord;
import au.org.raid.db.jooq.tables.records.RaidContributorPositionRecord;
import au.org.raid.idl.raidv2.model.ContributorPosition;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributorPositionServiceTest {
    @Mock
    private RaidContributorPositionRepository raidContributorPositionRepository;
    @Mock
    private RaidContributorPositionRecordFactory raidContributorPositionRecordFactory;
    @Mock
    private ContributorPositionFactory contributorPositionFactory;
    @Mock
    private ContributorPositionRepository contributorPositionRepository;
    @Mock
    private ContributorPositionSchemaRepository contributorPositionSchemaRepository;
    @InjectMocks
    private ContributorPositionService contributorPositionService;

    @Test
    @DisplayName("findAllByRaidContributorId() returns all contributor positions")
    void findAllByRaidContributorId() {
        final var raidContributorId = 123;
        final var contributorPositionId = 234;
        final var schemaId = 345;
        final var uri = "_uri";
        final var schemaUri = "schema-uri";
        final var contributorPositionRecord = new ContributorPositionRecord()
                .setUri(uri)
                .setSchemaId(schemaId);

        final var raidContributorPositionRecord = new RaidContributorPositionRecord()
                .setContributorPositionId(contributorPositionId);

        final var contributorPositionSchemaRecord = new ContributorPositionSchemaRecord()
                .setUri(schemaUri);

        final var contributorPosition = new ContributorPosition();

        when(raidContributorPositionRepository.findAllByRaidContributorId(raidContributorId))
                .thenReturn(List.of(raidContributorPositionRecord));

        when(contributorPositionRepository.findById(contributorPositionId))
                .thenReturn(Optional.of(contributorPositionRecord));

        when(contributorPositionSchemaRepository.findById(schemaId))
                .thenReturn(Optional.of(contributorPositionSchemaRecord));

        when(contributorPositionFactory.create(raidContributorPositionRecord, uri, schemaUri))
                .thenReturn(contributorPosition);

        assertThat(contributorPositionService.findAllByRaidContributorId(raidContributorId),
                is(List.of(contributorPosition)));
    }

    @Test
    @DisplayName("findAllByRaidContributorId() returns empty list when no positions")
    void findAllByRaidContributorIdReturnsEmptyList() {
        final var raidContributorId = 123;

        when(raidContributorPositionRepository.findAllByRaidContributorId(raidContributorId))
                .thenReturn(Collections.emptyList());

        assertThat(contributorPositionService.findAllByRaidContributorId(raidContributorId),
                is(Collections.emptyList()));

        verifyNoInteractions(contributorPositionRepository);
        verifyNoInteractions(contributorPositionSchemaRepository);
        verifyNoInteractions(contributorPositionFactory);
    }


    @Test
    @DisplayName("findAllByRaidContributorId() throws ContributorPositionNotFoundException")
    void findAllByRaidContributorIdThrowsContributorPositionNotFoundException() {
        final var raidContributorId = 123;
        final var contributorPositionId = 234;

        final var raidContributorPositionRecord = new RaidContributorPositionRecord()
                .setContributorPositionId(contributorPositionId);

        when(raidContributorPositionRepository.findAllByRaidContributorId(raidContributorId))
                .thenReturn(List.of(raidContributorPositionRecord));

        when(contributorPositionRepository.findById(contributorPositionId))
                .thenReturn(Optional.empty());

        assertThrows(ContributorPositionNotFoundException.class,
                () -> contributorPositionService.findAllByRaidContributorId(raidContributorId));

        verifyNoInteractions(contributorPositionSchemaRepository);
        verifyNoInteractions(contributorPositionFactory);
    }

    @Test
    @DisplayName("findAllByRaidContributorId() throws ContributorPositionSchemaNotFoundException")
    void findAllByRaidContributorIdThrowsContributorPositionSchemaNotFoundException() {
        final var raidContributorId = 123;
        final var contributorPositionId = 234;
        final var schemaId = 345;
        final var uri = "_uri";
        final var contributorPositionRecord = new ContributorPositionRecord()
                .setUri(uri)
                .setSchemaId(schemaId);

        final var raidContributorPositionRecord = new RaidContributorPositionRecord()
                .setContributorPositionId(contributorPositionId);

        when(raidContributorPositionRepository.findAllByRaidContributorId(raidContributorId))
                .thenReturn(List.of(raidContributorPositionRecord));

        when(contributorPositionRepository.findById(contributorPositionId))
                .thenReturn(Optional.of(contributorPositionRecord));

        when(contributorPositionSchemaRepository.findById(schemaId))
                .thenReturn(Optional.empty());

        assertThrows(ContributorPositionSchemaNotFoundException.class,
                () -> contributorPositionService.findAllByRaidContributorId(raidContributorId));

        verifyNoInteractions(contributorPositionFactory);
    }


    @Test
    @DisplayName("create() saves all contributor positions")
    void create() {
        final var raidContributorId = 123;
        final var uri = "_uri";
        final var schemaUri = "schema-uri";
        final var schemaId = 234;
        final var contributorPosition = new ContributorPosition()
                .id(uri)
                .schemaUri(schemaUri);
        final var contributorPositionId = 345;

        final var contributorPositionSchemaRecord = new ContributorPositionSchemaRecord()
                .setId(schemaId);

        final var contributorPositionRecord = new ContributorPositionRecord()
                .setId(contributorPositionId);

        final var raidContributorPositionRecord = new RaidContributorPositionRecord();

        when(contributorPositionSchemaRepository.findByUri(schemaUri))
                .thenReturn(Optional.of(contributorPositionSchemaRecord));

        when(contributorPositionRepository.findByUriAndSchemaId(uri, schemaId))
                .thenReturn(Optional.of(contributorPositionRecord));

        when(raidContributorPositionRecordFactory.create(contributorPosition, raidContributorId, contributorPositionId))
                .thenReturn(raidContributorPositionRecord);

        contributorPositionService.create(List.of(contributorPosition), raidContributorId);

        verify(raidContributorPositionRepository).create(raidContributorPositionRecord);
    }

    @Test
    @DisplayName("create() with null contributor positions does not save")
    void createWithNullContributorPositions() {
        final var raidContributorId = 123;

        contributorPositionService.create(null, raidContributorId);

        verifyNoInteractions(contributorPositionSchemaRepository);
        verifyNoInteractions(contributorPositionRepository);
        verifyNoInteractions(raidContributorPositionRecordFactory);
        verifyNoInteractions(raidContributorPositionRepository);
    }

    @Test
    @DisplayName("create() throws ContributorPositionSchemaNotFoundException")
    void createThrowsContributorPositionSchemaNotFoundException() {
        final var raidContributorId = 123;
        final var schemaUri = "schema-uri";
        final var contributorPosition = new ContributorPosition()
                .schemaUri(schemaUri);

        when(contributorPositionSchemaRepository.findByUri(schemaUri))
                .thenReturn(Optional.empty());

        assertThrows(ContributorPositionSchemaNotFoundException.class, () ->
                contributorPositionService.create(List.of(contributorPosition), raidContributorId));

        verifyNoInteractions(contributorPositionRepository);
        verifyNoInteractions(raidContributorPositionRecordFactory);
        verifyNoInteractions(raidContributorPositionRepository);
    }

    @Test
    @DisplayName("create() throws ContributorPositionNotFoundException")
    void createThrowsContributorPositionNotFoundException() {
        final var raidContributorId = 123;
        final var uri = "_uri";
        final var schemaUri = "schema-uri";
        final var schemaId = 234;
        final var contributorPosition = new ContributorPosition()
                .id(uri)
                .schemaUri(schemaUri);

        final var contributorPositionSchemaRecord = new ContributorPositionSchemaRecord()
                .setId(schemaId);

        when(contributorPositionSchemaRepository.findByUri(schemaUri))
                .thenReturn(Optional.of(contributorPositionSchemaRecord));

        when(contributorPositionRepository.findByUriAndSchemaId(uri, schemaId))
                .thenReturn(Optional.empty());

        assertThrows(ContributorPositionNotFoundException.class,
                () -> contributorPositionService.create(List.of(contributorPosition), raidContributorId));

        verifyNoInteractions(raidContributorPositionRecordFactory);
        verifyNoInteractions(raidContributorPositionRepository);
    }
}