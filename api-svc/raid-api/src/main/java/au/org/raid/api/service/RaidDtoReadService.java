package au.org.raid.api.service;

import au.org.raid.db.jooq.enums.Metaschema;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.idl.raidv2.model.RaidDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Encapsulates the read-optimised projection logic for list queries.
 *
 * For v2 raids, reads the current {@link RaidDto} directly from the materialised
 * {@code raid.metadata} column. Falls back to {@link RaidHistoryService} if the
 * materialised column is absent, empty, or unparseable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RaidDtoReadService {
    private final ObjectMapper objectMapper;
    private final RaidHistoryService raidHistoryService;

    /**
     * Returns the {@link RaidDto} for the given record using the materialised
     * metadata column when available, or an empty {@link Optional} if the record
     * cannot be resolved through either path.
     */
    public Optional<RaidDto> toRaidDto(final RaidRecord record) {
        if (Metaschema.raido_metadata_schema_v2.equals(record.getMetadataSchema())
                && record.getMetadata() != null) {
            try {
                final var raidDto = objectMapper.readValue(record.getMetadata().data(), RaidDto.class);
                if (raidDto.getIdentifier() != null) {
                    return Optional.of(raidDto);
                }
            } catch (final Exception e) {
                log.warn("Failed to deserialise materialised metadata for handle {}, falling back to history",
                        record.getHandle(), e);
            }
        }
        return raidHistoryService.findByHandle(record.getHandle());
    }
}
