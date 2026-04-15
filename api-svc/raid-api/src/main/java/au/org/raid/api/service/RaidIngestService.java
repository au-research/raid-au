package au.org.raid.api.service;

import au.org.raid.api.exception.ResourceNotFoundException;
import au.org.raid.api.factory.HandleFactory;
import au.org.raid.api.factory.RaidRecordFactory;
import au.org.raid.api.repository.RaidRepository;
import au.org.raid.api.util.TokenUtil;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.idl.raidv2.model.RaidDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RaidIngestService {
    private final TitleService titleService;
    private final DescriptionService descriptionService;
    private final ContributorService contributorService;
    private final OrganisationService organisationService;
    private final RelatedObjectService relatedObjectService;
    private final AlternateIdentifierService alternateIdentifierService;
    private final AlternateUrlService alternateUrlService;
    private final RelatedRaidService relatedRaidService;
    private final SubjectService subjectService;
    private final TraditionalKnowledgeLabelService traditionalKnowledgeLabelService;
    private final SpatialCoverageService spatialCoverageService;
    private final RaidRepository raidRepository;
    private final RaidRecordFactory raidRecordFactory;
    private final AccessService accessService;
    private final LanguageService languageService;
    private final HandleFactory handleFactory;
    private final CacheableRaidService cacheableRaidService;
    private final RaidHistoryService raidHistoryService;
    private final RaidDtoReadService raidDtoReadService;
    private final ObjectMapper objectMapper;

    public void create(final RaidDto raid) {
        final var handle = handleFactory.create(raid.getIdentifier().getId()).toString();

        final var accessTypeId = accessService.findAccessTypeId(raid.getAccess());

        Integer accessStatementLanguageId = null;
        if (raid.getAccess().getStatement() != null) {
            accessStatementLanguageId = languageService.findLanguageId(raid.getAccess().getStatement().getLanguage());
        }

        final Integer registrationAgencyOrganisationId = organisationService.findOrCreate(
                raid.getIdentifier().getRegistrationAgency().getId(),
                raid.getIdentifier().getRegistrationAgency().getSchemaUri()
        );

        final Integer ownerOrganisationId = organisationService.findOrCreate(
                raid.getIdentifier().getOwner().getId(),
                raid.getIdentifier().getOwner().getSchemaUri()
        );

        final var raidRecord = raidRecordFactory.create(raid, accessTypeId, accessStatementLanguageId,
                registrationAgencyOrganisationId, ownerOrganisationId);

        raidRepository.insert(raidRecord);

        titleService.create(raid.getTitle(), handle);
        descriptionService.create(raid.getDescription(), handle);
        contributorService.create(raid.getContributor(), handle);
        organisationService.create(raid.getOrganisation(), handle);
        relatedObjectService.create(raid.getRelatedObject(), handle);
        alternateIdentifierService.create(raid.getAlternateIdentifier(), handle);
        alternateUrlService.create(raid.getAlternateUrl(), handle);
        relatedRaidService.create(raid.getRelatedRaid(), handle);
        subjectService.create(raid.getSubject(), handle);
        spatialCoverageService.create(raid.getSpatialCoverage(), handle);
    }

    public List<RaidDto> findAllByServicePointIdOrNotConfidential(final Long servicePointId) {
        final var raids = new ArrayList<RaidDto>();
        final var records = raidRepository.findAllByServicePointIdOrNotConfidential(servicePointId);

        for (final var record : records) {
            raids.add(raidDtoReadService.toRaidDto(record)
                    .orElseGet(() -> cacheableRaidService.build(record)));
        }

        return raids;
    }

    public List<RaidDto> findAllByServicePointId(final Long servicePointId) {
        final var raids = new ArrayList<RaidDto>();
        final var records = raidRepository.findAllByServicePointId(servicePointId);

        for (final var record : records) {
            raids.add(raidDtoReadService.toRaidDto(record)
                    .orElseGet(() -> cacheableRaidService.build(record)));
        }

        return raids;
    }

    public List<RaidDto> findAllByContributor(final String contributorId) {
        final var raids = new ArrayList<RaidDto>();
        final var records = raidRepository.findAllByContributorOrcid(contributorId);

        for (final var record : records) {
            raids.add(raidDtoReadService.toRaidDto(record)
                    .orElseGet(() -> cacheableRaidService.build(record)));
        }

        return raids;
    }

    public List<RaidDto> findAllByOrganisation(final String organisationId) {
        final var raids = new ArrayList<RaidDto>();
        final var records = raidRepository.findAllByOrganisationId(organisationId);

        for (final var record : records) {
            raids.add(raidDtoReadService.toRaidDto(record)
                    .orElseGet(() -> cacheableRaidService.build(record)));
        }

        return raids;
    }

    public Optional<RaidDto> findByHandle(final String handle) {
        return raidHistoryService.findByHandle(handle)
                .or(Optional::empty);
    }

    public RaidDto update(final RaidDto raid) {
        final var handle = handleFactory.create(raid.getIdentifier().getId()).toString();

        if (raidRepository.findByHandle(handle).isEmpty()) {
            throw new ResourceNotFoundException(raid.getIdentifier().getId());
        }

        final var accessTypeId = accessService.findAccessTypeId(raid.getAccess());

        Integer accessStatementLanguageId = null;
        if (raid.getAccess().getStatement() != null) {
            accessStatementLanguageId = languageService.findLanguageId(raid.getAccess().getStatement().getLanguage());
        }

        final Integer registrationAgencyOrganisationId = organisationService.findOrCreate(
                raid.getIdentifier().getRegistrationAgency().getId(),
                raid.getIdentifier().getRegistrationAgency().getSchemaUri()
        );

        final Integer ownerOrganisationId = organisationService.findOrCreate(
                raid.getIdentifier().getOwner().getId(),
                raid.getIdentifier().getOwner().getSchemaUri()
        );

        final var raidRecord = raidRecordFactory.create(raid, accessTypeId, accessStatementLanguageId,
                registrationAgencyOrganisationId, ownerOrganisationId);

        raidRepository.update(raidRecord);

        titleService.update(raid.getTitle(), handle);
        descriptionService.update(raid.getDescription(), handle);
        contributorService.update(raid.getContributor(), handle);
        organisationService.update(raid.getOrganisation(), handle);
        relatedObjectService.update(raid.getRelatedObject(), handle);
        alternateIdentifierService.update(raid.getAlternateIdentifier(), handle);
        alternateUrlService.update(raid.getAlternateUrl(), handle);
        relatedRaidService.update(raid.getRelatedRaid(), handle);
        subjectService.update(raid.getSubject(), handle);
        spatialCoverageService.update(raid.getSpatialCoverage(), handle);

        return raid;
    }

    public List<RaidDto> findAllByServicePointIdOrHandleIn(final Long servicePointId) {
        final var handles = new ArrayList<>(TokenUtil.getAdminRaids());
        handles.addAll(TokenUtil.getUserRaids());

        final var isServicePointUser = TokenUtil.hasRole(TokenUtil.SERVICE_POINT_USER_ROLE);

        return raidRepository.findAllViewable(servicePointId, isServicePointUser, handles)
                .stream().map(RaidRecord::getMetadata)
                .map(raidDto -> {
                    try {
                        return objectMapper.readValue(raidDto.data(), RaidDto.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

    public List<RaidDto> findAll() {
        return raidRepository.findAll().stream()
                .map(RaidRecord::getMetadata)
                .map(raidDto -> {
                    try {
                        return objectMapper.readValue(raidDto.data(), RaidDto.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }

    public List<RaidDto> findAllIncludingWithoutHistory() {
        return raidRepository.findAllIncludingWithoutHistory().stream()
                .map(RaidRecord::getMetadata)
                .map(metadata -> {
                    try {
                        return objectMapper.readValue(metadata.data(), RaidDto.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
}
