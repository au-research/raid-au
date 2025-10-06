package au.org.raid.api.service;

import au.org.raid.api.config.properties.DataciteProperties;
import au.org.raid.api.dto.LegacyRaid;
import au.org.raid.api.dto.legacy.RaidDtoFactory;
import au.org.raid.api.entity.ChangeType;
import au.org.raid.api.factory.*;
import au.org.raid.api.factory.datacite.DataciteRequestFactory;
import au.org.raid.api.model.datacite.DataciteRelatedIdentifier;
import au.org.raid.api.model.datacite.DataciteRequest;
import au.org.raid.api.repository.*;
import au.org.raid.api.service.datacite.DataciteService;
import au.org.raid.idl.raidv2.model.RaidDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegacyRaidService {
    public static final String EMPTY_JSON = "{}";

    private final RaidRepository raidRepository;
    private final ObjectMapper objectMapper;
    private final RaidDtoFactory raidDtoFactory;
    private final DataciteRequestFactory dataciteRequestFactory;
    private final ServicePointRepository servicePointRepository;
    private final RestTemplate restTemplate;
    private final HttpEntityFactory httpEntityFactory;
    private final DataciteProperties properties;
    private final JsonPatchFactory jsonPatchFactory;
    private final RaidHistoryRepository raidHistoryRepository;
    private final RaidHistoryRecordFactory raidHistoryRecordFactory;
    private final IdFactory idFactory;
    private final RaidIngestService raidIngestService;
    private final RaidTitleRepository raidTitleRepository;
    private final RaidDescriptionRepository raidDescriptionRepository;

    @SneakyThrows
    public List<LegacyRaid> findAll() {
        return raidRepository.findAllLegacy()
                .stream().map(record -> {
                    try {
                        return objectMapper.readValue(record.getMetadata().data(), LegacyRaid.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    @SneakyThrows
    @Transactional
    public RaidDto upgrade(final LegacyRaid legacyRaid) {
        final var servicePointId = legacyRaid.getId().getIdentifierServicePoint();

        final var servicePoint = servicePointRepository.findById(servicePointId)
                .orElseThrow(() -> new RuntimeException("No such service point %d".formatted(servicePointId)));

        final var raidDto = raidDtoFactory.create(legacyRaid);

        final var id = legacyRaid.getId().getIdentifier();
        final var oldHandle = id.substring(id.indexOf('/', 8) + 1);

        final var suffix = id.substring(id.lastIndexOf('/') + 1);

        final var handle = new Handle(servicePoint.getPrefix(), suffix);

        raidDto.setIdentifier(idFactory.create(handle.toString(), servicePoint));
        raidDto.getIdentifier().version(2);

        final var raidString = objectMapper.writeValueAsString(legacyRaid);

        final var diff = jsonPatchFactory.create(EMPTY_JSON, raidString);

        raidHistoryRepository.insert(raidHistoryRecordFactory.create(handle, 1, ChangeType.PATCH, diff));

        final var dataciteRequest = dataciteRequestFactory.create(raidDto, handle.toString());

        dataciteRequest.getData().getAttributes().getRelatedIdentifiers()
                .add(new DataciteRelatedIdentifier()
                        .setRelatedIdentifier(id)
                        .setRelationType("IsIdenticalTo")
                        .setRelatedIdentifierType("Handle")
                );

        log.debug("POSTing Datacite request: {}", objectMapper.writeValueAsString(dataciteRequest));

        final HttpEntity<DataciteRequest> entity = httpEntityFactory.create(dataciteRequest, servicePoint.getRepositoryId(), servicePoint.getPassword());
        log.debug("Making POST request to Datacite: {}", properties.getEndpoint());

        try {
            restTemplate.exchange(properties.getEndpoint(), HttpMethod.POST, entity, JsonNode.class);
        } catch (HttpClientErrorException e) {
            log.error("Unable to create Datacite record", e);
        }

        final var updateDiff = jsonPatchFactory.create(raidString, objectMapper.writeValueAsString(raidDto));

        raidHistoryRepository.insert(raidHistoryRecordFactory.create(handle, 2, ChangeType.PATCH, updateDiff));

        raidIngestService.create(raidDto);

        raidTitleRepository.deleteAllByHandle(oldHandle);
        raidDescriptionRepository.deleteAllByHandle(oldHandle);
        raidRepository.deleteByHandle(oldHandle);

        return raidDto;
    }
}
