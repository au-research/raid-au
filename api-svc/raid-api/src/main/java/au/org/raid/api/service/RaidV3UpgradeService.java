package au.org.raid.api.service;

import au.org.raid.api.factory.JsonValueFactory;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.api.repository.RaidHistoryRepository;
import au.org.raid.api.repository.RaidRepository;
import au.org.raid.db.jooq.tables.records.RaidHistoryRecord;
import au.org.raid.idl.raidv2.model.RaidDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Transactional
public class RaidV3UpgradeService {
    private final RaidHistoryRepository raidHistoryRepository;
    private final RaidRepository raidRepository;
    private final JsonValueFactory jsonValueFactory;
    private final ObjectMapper objectMapper;
    private final CacheableRaidService raidService;
    private final ContributorRepository contributorRepository;


    public List<RaidDto> upgrade() {
        final List<Map<?, ?>> raids = findAllRaids();

        return raids.stream()
                .peek(raid -> {
                    upgradeRelatedObjectCategories(raid);
                    upgradeSpatialCoveragePlaces(raid);
                })
                .map(raid -> {
                    try {
                        return objectMapper.writeValueAsString(raid);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(raid -> {
                    try {
                        return objectMapper.readValue(raid, RaidDto.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .peek(raid -> {
                    raid.getContributor().forEach(contributor -> {
                        if (contributor.getStatus() == null) {
                            contributor.setStatus("UNAUTHENTICATED");
                        }
                        if (contributor.getUuid() == null) {
                            final var uuid = UUID.randomUUID().toString();

                            contributor.setUuid(uuid);

                            final var orcid = contributor.getId().substring(contributor.getId().lastIndexOf('/') + 1);

                            final var contributorRecord = contributorRepository.findByPid(contributor.getId())
                                    .orElseThrow(() -> new RuntimeException("Contributor not found with orcid %s".formatted(orcid)));
                            contributorRecord.setStatus("UNAUTHENTICATED");

                            contributorRepository.update(contributorRecord);
                        }
                    });
                })
                .toList();
    }


    private void upgradeRelatedObjectCategories(Map<?, ?> raid) {
        final var relatedObjects = ((List<?>) raid.get("relatedObject"));

        if (relatedObjects != null) {
            relatedObjects.forEach(relatedObject -> {
                final var category = ((LinkedHashMap<?, ?>) relatedObject).get("category");
                if (category instanceof LinkedHashMap) {
                    ((LinkedHashMap) relatedObject).put("category", List.of(category));
                }
            });
        }
    }


    private void upgradeSpatialCoveragePlaces(Map<?, ?> raid) {
        final var spatialCoverages = ((List<?>) raid.get("spatialCoverage"));

        if (spatialCoverages != null) {
            spatialCoverages.forEach(spatialCoverage -> {
                final var place = ((LinkedHashMap<?, ?>) spatialCoverage).get("place");
                if (place instanceof String) {
                    final var placeMap = new LinkedHashMap<>(Map.of("text", place));
                    ((LinkedHashMap) spatialCoverage).put("place", List.of(placeMap));
                }
            });
        }
    }

    @SneakyThrows
    public Optional<Map<?, ?>> findByHandle(final String handle) {
        Map<?, ?> raidMap = null;

        final var history = raidHistoryRepository.findAllByHandle(handle).stream()
                .map(RaidHistoryRecord::getDiff)
                .map(jsonValueFactory::create)
                .toList();

        if (history.isEmpty()) {
            final var raidOptional = raidRepository.findByHandle(handle).map(record -> {
                try {
                    final var raidDto = raidService.build(record);
                    return objectMapper.readValue(objectMapper.writeValueAsString(raidDto), Map.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });

            if (raidOptional.isPresent()) {
                raidMap = raidOptional.get();
            }

        } else {
            raidMap = objectMapper.readValue(jsonValueFactory.create(history).toString(), Map.class);
        }

        return Optional.ofNullable(raidMap);
    }

    public List<Map<?,?>> findAllRaids() {
        final var raids = new ArrayList<Map<?,?>>();
        final var records = raidRepository.findAllV2();

        for (final var record : records) {
            final var raidOptional = findByHandle(record.getHandle());

            raidOptional.ifPresent(raids::add);
        }

        return raids;
    }
}
