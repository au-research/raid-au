package au.org.raid.api.service;

import au.org.raid.api.factory.DateFactory;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.idl.raidv2.model.RaidDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CacheableRaidService {
    private final TitleService titleService;
    private final DescriptionService descriptionService;
    private final ContributorService contributorService;
    private final OrganisationService organisationService;
    private final RelatedObjectService relatedObjectService;
    private final AlternateIdentifierService alternateIdentifierService;
    private final AlternateUrlService alternateUrlService;
    private final RelatedRaidService relatedRaidService;
    private final SubjectService subjectService;
    private final SpatialCoverageService spatialCoverageService;
    private final AccessService accessService;
    private final IdService idService;
    private final DateFactory dateFactory;

    @Cacheable(value="raid-cache", key="{#record.handle, #record.version}")
    public RaidDto build(final RaidRecord record) {
        final var handle = record.getHandle();

        final var titles = titleService.findAllByHandle(handle);
        final var descriptions = descriptionService.findAllByHandle(handle);
        final var contributors = contributorService.findAllByHandle(handle);
        final var organisations = organisationService.findAllByHandle(handle);
        final var relatedObjects = relatedObjectService.findAllByHandle(handle);
        final var alternateIdentifiers = alternateIdentifierService.findAllByHandle(handle);
        final var alternateUrls = alternateUrlService.findAllByHandle(handle);
        final var relatedRaids = relatedRaidService.findAllByHandle(handle);
        final var subjects = subjectService.findAllByHandle(handle);
        final var spatialCoverages = spatialCoverageService.findAllByHandle(handle);

        return new RaidDto()
                .identifier(idService.getId(record))
                .date(dateFactory.create(record.getStartDateString(), record.getEndDate()))
                .title(titles.isEmpty() ? null : titles)
                .description(descriptions.isEmpty() ? null : descriptions)
                .contributor(contributors.isEmpty() ? null : contributors)
                .organisation(organisations.isEmpty() ? null: organisations)
                .relatedObject(relatedObjects.isEmpty() ? null : relatedObjects)
                .alternateIdentifier(alternateIdentifiers.isEmpty() ? null : alternateIdentifiers)
                .alternateUrl(alternateUrls.isEmpty() ? null : alternateUrls)
                .relatedRaid(relatedRaids.isEmpty() ? null : relatedRaids)
                .access(accessService.getAccess(record))
                .subject(subjects.isEmpty() ? null : subjects)
                .spatialCoverage(spatialCoverages.isEmpty() ? null : spatialCoverages);
    }
}
