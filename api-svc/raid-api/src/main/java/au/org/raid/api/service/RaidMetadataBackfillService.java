package au.org.raid.api.service;

import au.org.raid.api.dto.BackfillResult;
import au.org.raid.api.repository.RaidRepository;
import au.org.raid.idl.raidv2.model.RaidDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RaidMetadataBackfillService {
    private final RaidRepository raidRepository;
    private final RaidHistoryService raidHistoryService;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public BackfillResult backfill() {
        final var allV2Records = raidRepository.findAllV2();
        var backfilledCount = 0;
        var skippedCount = 0;

        log.info("Starting raid metadata backfill for {} v2 raids", allV2Records.size());

        for (final var record : allV2Records) {
            try {
                if (hasValidMaterialisedMetadata(record.getMetadata() != null ? record.getMetadata().data() : null)) {
                    skippedCount++;
                    continue;
                }

                final var raidDtoOptional = raidHistoryService.findByHandle(record.getHandle());
                if (raidDtoOptional.isEmpty()) {
                    log.warn("Could not reconstruct RaidDto for handle {}, skipping backfill", record.getHandle());
                    skippedCount++;
                    continue;
                }

                final var json = objectMapper.writeValueAsString(raidDtoOptional.get());
                final var rowsUpdated = raidRepository.updateMetadata(record.getHandle(), json);
                if (rowsUpdated == 0) {
                    log.warn("Failed to materialise metadata for handle {}: no matching raid record", record.getHandle());
                } else {
                    backfilledCount++;
                }
            } catch (final Exception e) {
                log.warn("Failed to backfill handle {}: {}", record.getHandle(), e.getMessage());
                skippedCount++;
            }
        }

        log.info("Raid metadata backfill complete: {} backfilled, {} skipped", backfilledCount, skippedCount);

        return BackfillResult.builder()
                .total(allV2Records.size())
                .backfilled(backfilledCount)
                .skipped(skippedCount)
                .build();
    }

    private boolean hasValidMaterialisedMetadata(final String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return false;
        }
        try {
            final var raidDto = objectMapper.readValue(metadataJson, RaidDto.class);
            return raidDto.getIdentifier() != null;
        } catch (final Exception e) {
            return false;
        }
    }
}
