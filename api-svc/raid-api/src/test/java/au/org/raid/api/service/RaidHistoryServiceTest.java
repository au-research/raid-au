package au.org.raid.api.service;

import au.org.raid.api.config.properties.RaidHistoryProperties;
import au.org.raid.api.entity.ChangeType;
import au.org.raid.api.exception.InvalidVersionException;
import au.org.raid.api.factory.*;
import au.org.raid.api.repository.RaidHistoryRepository;
import au.org.raid.api.repository.RaidRepository;
import au.org.raid.db.jooq.tables.records.RaidHistoryRecord;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.idl.raidv2.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonPatch;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RaidHistoryServiceTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private RaidHistoryRepository raidHistoryRepository;
    @Mock
    private JsonPatchFactory jsonPatchFactory;
    @Mock
    private JsonValueFactory jsonValueFactory;
    @Mock
    private HandleFactory handleFactory;
    @Mock
    private RaidHistoryRecordFactory raidHistoryRecordFactory;
    @Mock
    private RaidHistoryProperties raidHistoryProperties;
    @Mock
    private RaidChangeFactory raidChangeFactory;
    @Mock
    private RaidRepository raidRepository;
    @InjectMocks
    private RaidHistoryService raidHistoryService;

    @Test
    @DisplayName("Throws InvalidVersionException if version already exists")
    void throwsInvalidVersionException() throws JsonProcessingException {
        final var revision = 123;
        final var request = new RaidUpdateRequest();
        final var identifier = mock(Id.class);
        request.setIdentifier(identifier);
        final var raidJson = "{}";
        final var handleString = "123.456/abcde";
        final var handle = new Handle(handleString);
        final var diff = mock(JsonPatch.class);
        final var patchRaidHistoryRecord = new RaidHistoryRecord();
        final var history1 = new RaidHistoryRecord();
        final var history2 = new RaidHistoryRecord();
        final var diffString1 = "diff1";
        final var diffString2 = "diff2";
        history1.setDiff(diffString1);
        history2.setDiff(diffString2);
        final var history1Diff = mock(JsonValue.class);
        final var history2Diff = mock(JsonValue.class);
        final var existingRaid = mock(JsonValue.class);
        final var existingRaidString = "existing-raid";
        final var raidRecord = new RaidRecord();
        raidRecord.setDateCreated(LocalDateTime.now());

        when(raidRepository.findByHandle(handleString)).thenReturn(Optional.of(raidRecord));

        when(jsonValueFactory.create(diffString1)).thenReturn(history1Diff);
        when(jsonValueFactory.create(diffString2)).thenReturn(history2Diff);

        when(jsonValueFactory.create(List.of(history1Diff, history2Diff))).thenReturn(existingRaid);
        when(existingRaid.toString()).thenReturn(existingRaidString);

        when(objectMapper.writeValueAsString(request)).thenReturn(raidJson);
        when(identifier.getId()).thenReturn(handleString);
        when(identifier.getVersion()).thenReturn(revision);
        when(handleFactory.create(handleString)).thenReturn(handle);

        when(raidHistoryRepository.findAllByHandle(handleString)).thenReturn(List.of(history1, history2));
        when(jsonPatchFactory.create(existingRaidString, raidJson)).thenReturn(diff);
        when(raidHistoryRecordFactory.create(eq(handle), eq(124), eq(ChangeType.PATCH), eq(diff))).thenReturn(patchRaidHistoryRecord);

        when(raidHistoryRepository.insert(patchRaidHistoryRecord)).thenReturn(0);

        assertThrows(InvalidVersionException.class, () -> raidHistoryService.save(request));

        verify(raidHistoryRepository).insert(patchRaidHistoryRecord);
    }
    @Nested
    class CreateTests {
        @Test
        @DisplayName("Saves on create")
        void saveOnCreate() throws JsonProcessingException {
            final var request = new RaidCreateRequest();
            final var identifier = mock(Id.class);
            request.setIdentifier(identifier);
            final var raidJson = "{}";
            final var handleString = "123.456/abcde";
            final var handle = new Handle(handleString);
            final var diff = mock(JsonPatch.class);
            final var raidHistoryRecord = new RaidHistoryRecord();
            final var raidJsonValue = mock(JsonValue.class);
            final var raidDto = new RaidDto();
            final var raidJsonValueAsString = "raid-json-value";

            when(objectMapper.writeValueAsString(request)).thenReturn(raidJson);
            when(identifier.getId()).thenReturn(handleString);
            when(handleFactory.create(handleString)).thenReturn(handle);
            when(jsonPatchFactory.create(eq("{}"), eq(raidJson))).thenReturn(diff);
            when(raidHistoryRecordFactory.create(eq(handle), eq(1), eq(ChangeType.PATCH), eq(diff))).thenReturn(raidHistoryRecord);
            when(jsonValueFactory.create(diff)).thenReturn(raidJsonValue);
            when(raidJsonValue.toString()).thenReturn(raidJsonValueAsString);
            when(objectMapper.readValue(eq(raidJsonValueAsString), eq(RaidDto.class))).thenReturn(raidDto);

            final var result = raidHistoryService.save(request);

            assertThat(result, is(raidDto));

            verify(raidHistoryRepository).insert(raidHistoryRecord);
        }
    }

    @Nested
    class UpdateTests {
        @BeforeEach
        void setUp() {
            when(raidHistoryProperties.getBaselineInterval()).thenReturn(5);
        }

        @Test
        @DisplayName("Saves on update")
        void saveOnUpdate() throws JsonProcessingException {
            final var revision = 123;
            final var request = new RaidUpdateRequest();
            final var identifier = mock(Id.class);
            request.setIdentifier(identifier);
            final var raidJson = "{}";
            final var handleString = "123.456/abcde";
            final var handle = new Handle(handleString);
            final var diff = mock(JsonPatch.class);
            final var patchRaidHistoryRecord = new RaidHistoryRecord();
            final var raidJsonValue = mock(JsonValue.class);
            final var raidDto = new RaidDto();
            final var raidJsonValueAsString = "raid-json-value";
            final var history1 = new RaidHistoryRecord();
            final var history2 = new RaidHistoryRecord();
            final var diffString1 = "diff1";
            final var diffString2 = "diff2";
            history1.setDiff(diffString1);
            history2.setDiff(diffString2);
            final var history1Diff = mock(JsonValue.class);
            final var history2Diff = mock(JsonValue.class);
            final var existingRaid = mock(JsonValue.class);
            final var existingRaidString = "existing-raid";
            final var raidRecord = new RaidRecord();
            raidRecord.setDateCreated(LocalDateTime.now());

            when(raidRepository.findByHandle(handleString)).thenReturn(Optional.of(raidRecord));

            when(jsonValueFactory.create(diffString1)).thenReturn(history1Diff);
            when(jsonValueFactory.create(diffString2)).thenReturn(history2Diff);

            when(jsonValueFactory.create(List.of(history1Diff, history2Diff))).thenReturn(existingRaid);
            when(existingRaid.toString()).thenReturn(existingRaidString);

            when(objectMapper.writeValueAsString(request)).thenReturn(raidJson);
            when(identifier.getId()).thenReturn(handleString);
            when(identifier.getVersion()).thenReturn(revision);
            when(handleFactory.create(handleString)).thenReturn(handle);

            when(raidHistoryRepository.findAllByHandle(handleString)).thenReturn(List.of(history1, history2));
            when(jsonPatchFactory.create(existingRaidString, raidJson)).thenReturn(diff);
            when(raidHistoryRecordFactory.create(eq(handle), eq(124), eq(ChangeType.PATCH), eq(diff))).thenReturn(patchRaidHistoryRecord);

            when(jsonValueFactory.create(List.of(history1Diff, history2Diff), diff)).thenReturn(raidJsonValue);
            when(raidJsonValue.toString()).thenReturn(raidJsonValueAsString);
            when(objectMapper.readValue(eq(raidJsonValueAsString), eq(RaidDto.class))).thenReturn(raidDto);
            when(raidHistoryRepository.insert(patchRaidHistoryRecord)).thenReturn(1);

            final var result = raidHistoryService.save(request);

            assertThat(result, is(raidDto));

            verify(raidHistoryRepository).insert(patchRaidHistoryRecord);
        }

        @Test
        @DisplayName("Saves baseline on update")
        void saveBaselineOnUpdate() throws JsonProcessingException {
            final var revision = 4;
            final var request = new RaidUpdateRequest();
            final var identifier = mock(Id.class);
            request.setIdentifier(identifier);
            final var raidJson = "{}";
            final var handleString = "123.456/abcde";
            final var handle = new Handle(handleString);
            final var diff = mock(JsonPatch.class);
            final var baselineDiff = mock(JsonPatch.class);
            final var patchRaidHistoryRecord = new RaidHistoryRecord().setChangeType(ChangeType.PATCH.name());
            final var baselineRaidHistoryRecord = new RaidHistoryRecord().setChangeType(ChangeType.BASELINE.name());
            final var raidJsonValue = mock(JsonValue.class);
            final var raidDto = new RaidDto();
            final var raidJsonValueAsString = "raid-json-value";
            final var history1 = new RaidHistoryRecord();
            final var history2 = new RaidHistoryRecord();
            final var diffString1 = "diff1";
            final var diffString2 = "diff2";
            history1.setDiff(diffString1);
            history2.setDiff(diffString2);
            final var history1Diff = mock(JsonValue.class);
            final var history2Diff = mock(JsonValue.class);
            final var existingRaid = mock(JsonValue.class);
            final var existingRaidString = "existing-raid";
            final var raidRecord = new RaidRecord();
            raidRecord.setDateCreated(LocalDateTime.now());

            when(raidRepository.findByHandle(handleString)).thenReturn(Optional.of(raidRecord));

            when(jsonValueFactory.create(diffString1)).thenReturn(history1Diff);
            when(jsonValueFactory.create(diffString2)).thenReturn(history2Diff);

            when(jsonValueFactory.create(List.of(history1Diff, history2Diff))).thenReturn(existingRaid);
            when(existingRaid.toString()).thenReturn(existingRaidString);

            when(objectMapper.writeValueAsString(request)).thenReturn(raidJson);
            when(identifier.getId()).thenReturn(handleString);
            when(identifier.getVersion()).thenReturn(revision);
            when(handleFactory.create(handleString)).thenReturn(handle);

            when(raidHistoryRepository.findAllByHandle(handleString)).thenReturn(List.of(history1, history2));
            when(jsonPatchFactory.create(existingRaidString, raidJson)).thenReturn(diff);
            when(raidHistoryRecordFactory.create(eq(handle), eq(5), eq(ChangeType.PATCH), eq(diff))).thenReturn(patchRaidHistoryRecord);

            when(jsonPatchFactory.create("{}", "{}")).thenReturn(baselineDiff);
            when(raidHistoryRecordFactory.create(eq(handle), eq(5), eq(ChangeType.BASELINE), eq(baselineDiff))).thenReturn(baselineRaidHistoryRecord);

            when(jsonValueFactory.create(List.of(history1Diff, history2Diff), diff)).thenReturn(raidJsonValue);
            when(raidJsonValue.toString()).thenReturn(raidJsonValueAsString);
            when(objectMapper.readValue(eq(raidJsonValueAsString), eq(RaidDto.class))).thenReturn(raidDto);

            when(raidHistoryRepository.insert(patchRaidHistoryRecord)).thenReturn(1);

            final var result = raidHistoryService.save(request);

            assertThat(result, is(raidDto));

            verify(raidHistoryRepository).insert(patchRaidHistoryRecord);
            verify(raidHistoryRepository).insert(baselineRaidHistoryRecord);
        }
    }

    @Test
    @DisplayName("read() returns raid at correct version")
    void read() throws JsonProcessingException {
        final var version = 2;
        final var handle = "_handle";
        final var diff = "[]";
        final var raidString = "{}";

        final var raidHistoryRecord = new RaidHistoryRecord()
                .setDiff(diff);
        final var jsonValue = mock(JsonValue.class);

        final var raidJsonValue = mock(JsonValue.class);
        when(raidJsonValue.toString()).thenReturn(raidString);

        when(raidHistoryRepository.findAllByHandleAndVersion(handle, version)).thenReturn(List.of(raidHistoryRecord));
        when(jsonValueFactory.create(diff)).thenReturn(jsonValue);
        when(jsonValueFactory.create(List.of(jsonValue))).thenReturn(raidJsonValue);

        assertThat(raidHistoryService.findByHandleAndVersion(handle, version).get(), is(raidString));
    }

    @Test
    @DisplayName("findAllByHandleAndChangeType returns list of changes")
    void findAllByHandleAndChangeType() {
        final var handle = "_handle";
        final var changeType = "PATCH";

        final var raidHistoryRecord = new RaidHistoryRecord();
        final var change = new RaidChange();

        when(raidHistoryRepository.findAllByHandleAndChangeType(handle, changeType)).thenReturn(List.of(raidHistoryRecord));
        when(raidChangeFactory.create(raidHistoryRecord)).thenReturn(change);

        assertThat(raidHistoryService.findAllChangesByHandle(handle), is(List.of(change)));
    }
}