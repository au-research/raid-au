package au.org.raid.api.service;

import au.org.raid.db.jooq.enums.Metaschema;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.idl.raidv2.model.Id;
import au.org.raid.idl.raidv2.model.RaidDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jooq.JSONB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.SimpleDateFormat;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RaidDtoReadServiceTest {
    private static final String HANDLE = "10.26193/ABC123";

    @Mock
    RaidHistoryService raidHistoryService;
    @Spy
    ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    @InjectMocks
    RaidDtoReadService raidDtoReadService;

    @Nested
    class HappyPath {
        @Test
        @DisplayName("returns deserialised RaidDto from materialised metadata when schema is v2 and identifier is present")
        void returnsFromMaterialisedMetadata() throws Exception {
            final var expected = new RaidDto().identifier(new Id().id("https://raid.org/" + HANDLE));
            final var json = objectMapper.writeValueAsString(expected);

            final var record = new RaidRecord()
                    .setHandle(HANDLE)
                    .setMetadataSchema(Metaschema.raido_metadata_schema_v2)
                    .setMetadata(JSONB.valueOf(json));

            final var result = raidDtoReadService.toRaidDto(record);

            assertThat(result, is(Optional.of(expected)));
            verifyNoInteractions(raidHistoryService);
        }
    }

    @Nested
    class FallbackToHistory {
        @Test
        @DisplayName("delegates to raidHistoryService when metadata column is null")
        void fallsBackWhenMetadataNull() {
            final var expected = new RaidDto().identifier(new Id().id("https://raid.org/" + HANDLE));

            final var record = new RaidRecord()
                    .setHandle(HANDLE)
                    .setMetadataSchema(Metaschema.raido_metadata_schema_v2)
                    .setMetadata(null);

            when(raidHistoryService.findByHandle(HANDLE)).thenReturn(Optional.of(expected));

            final var result = raidDtoReadService.toRaidDto(record);

            assertThat(result, is(Optional.of(expected)));
            verify(raidHistoryService).findByHandle(HANDLE);
        }

        @Test
        @DisplayName("delegates to raidHistoryService when metadata schema is legacy")
        void fallsBackWhenLegacySchema() {
            final var expected = new RaidDto().identifier(new Id().id("https://raid.org/" + HANDLE));

            final var record = new RaidRecord()
                    .setHandle(HANDLE)
                    .setMetadataSchema(Metaschema.legacy_metadata_schema_v1)
                    .setMetadata(JSONB.valueOf("{}"));

            when(raidHistoryService.findByHandle(HANDLE)).thenReturn(Optional.of(expected));

            final var result = raidDtoReadService.toRaidDto(record);

            assertThat(result, is(Optional.of(expected)));
            verify(raidHistoryService).findByHandle(HANDLE);
        }

        @Test
        @DisplayName("delegates to raidHistoryService when metadata has no identifier")
        void fallsBackWhenMetadataHasNoIdentifier() throws Exception {
            final var expected = new RaidDto().identifier(new Id().id("https://raid.org/" + HANDLE));
            final var emptyJson = objectMapper.writeValueAsString(new RaidDto());

            final var record = new RaidRecord()
                    .setHandle(HANDLE)
                    .setMetadataSchema(Metaschema.raido_metadata_schema_v2)
                    .setMetadata(JSONB.valueOf(emptyJson));

            when(raidHistoryService.findByHandle(HANDLE)).thenReturn(Optional.of(expected));

            final var result = raidDtoReadService.toRaidDto(record);

            assertThat(result, is(Optional.of(expected)));
            verify(raidHistoryService).findByHandle(HANDLE);
        }

        @Test
        @DisplayName("delegates to raidHistoryService when metadata contains invalid JSON")
        void fallsBackWhenMetadataIsInvalidJson() {
            final var expected = new RaidDto().identifier(new Id().id("https://raid.org/" + HANDLE));

            final var record = new RaidRecord()
                    .setHandle(HANDLE)
                    .setMetadataSchema(Metaschema.raido_metadata_schema_v2)
                    .setMetadata(JSONB.valueOf("not-valid-json{{{"));

            when(raidHistoryService.findByHandle(HANDLE)).thenReturn(Optional.of(expected));

            final var result = raidDtoReadService.toRaidDto(record);

            assertThat(result, is(Optional.of(expected)));
            verify(raidHistoryService).findByHandle(HANDLE);
        }
    }
}
