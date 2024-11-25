package au.org.raid.api.service;

import au.org.raid.api.exception.ResourceNotFoundException;
import au.org.raid.api.factory.HandleFactory;
import au.org.raid.api.factory.RaidRecordFactory;
import au.org.raid.api.repository.RaidRepository;
import au.org.raid.idl.raidv2.model.RaidDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        traditionalKnowledgeLabelService.create(raid.getTraditionalKnowledgeLabel(), handle);
        spatialCoverageService.create(raid.getSpatialCoverage(), handle);
    }

    public List<RaidDto> findAllByServicePointIdOrNotConfidential(final Long servicePointId) {
        final var raids = new ArrayList<RaidDto>();
        final var records = raidRepository.findAllByServicePointIdOrNotConfidential(servicePointId);

        for (final var record : records) {
            raids.add(cacheableRaidService.build(record));
        }

        return raids;
    }

    public List<RaidDto> findAllByServicePointId(final Long servicePointId) {
        final var raids = new ArrayList<RaidDto>();
        final var records = raidRepository.findAllByServicePointId(servicePointId);

        for (final var record : records) {
            raids.add(cacheableRaidService.build(record));
        }

        return raids;
    }

    public List<RaidDto> findAllByContributor(final String id) {
        final String orcid = "https://orcid.org/%s".formatted(id);

        final var raids = new ArrayList<RaidDto>();
        final var records = raidRepository.findAllByContributorOrcid(orcid);

        for (final var record : records) {
            raids.add(cacheableRaidService.build(record));
        }

        return raids;
    }

    public List<RaidDto> findAllByOrganisation(final String id) {
        final String ror = "https://ror.org/%s".formatted(id);

        final var raids = new ArrayList<RaidDto>();
        final var records = raidRepository.findAllByOrganisationId(ror);

        for (final var record : records) {
            raids.add(cacheableRaidService.build(record));
        }

        return raids;
    }

    public Optional<RaidDto> findByHandle(final String handle) {
        return raidRepository.findByHandle(handle)
                .map(cacheableRaidService::build)
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
        traditionalKnowledgeLabelService.update(raid.getTraditionalKnowledgeLabel(), handle);
        spatialCoverageService.update(raid.getSpatialCoverage(), handle);

        return raid;
    }
}
