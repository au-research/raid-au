package au.org.raid.api.service;

import au.org.raid.api.config.properties.DataciteProperties;
import au.org.raid.api.entity.ChangeType;
import au.org.raid.api.factory.HttpEntityFactory;
import au.org.raid.api.factory.IdFactory;
import au.org.raid.api.factory.JsonPatchFactory;
import au.org.raid.api.factory.RaidHistoryRecordFactory;
import au.org.raid.api.factory.datacite.DataciteRequestFactory;
import au.org.raid.api.model.datacite.doi.DataciteRelatedIdentifier;
import au.org.raid.api.model.datacite.doi.DataciteRequest;
import au.org.raid.api.repository.*;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.idl.raidv2.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: Map contact/leader contributor positions to contact/leader properties on contributor

@Slf4j
@Component
@RequiredArgsConstructor
public class RaidUpgradeService {
    private static final String LEAD_CONTRIBUTOR_POSITION_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/leader.json";

    private static final String CONTACT_CONTRIBUTOR_POSITION_ID =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/contact.json";

    private static final Map<String, String> ACCESS_TYPE_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json",
            "https://vocabularies.coar-repositories.org/access_rights/c_f1cf/",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/embargoed.json",
            "https://vocabularies.coar-repositories.org/access_rights/c_f1cf/",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/open.json",
            "https://vocabularies.coar-repositories.org/access_rights/c_abf2/"
    );

    private static final Map<String, String> ACCESS_TYPE_SCHEMA_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/tree/main/scheme/access/type/v1/",
            "https://vocabularies.coar-repositories.org/access_rights/"
    );

    private static final Map<String, String> CONTRIBUTOR_POSITION_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/co-investigator.json",
            "https://vocabulary.raid.org/contributor.position.schema/308",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/other-participant.json",
            "https://vocabulary.raid.org/contributor.position.schema/311",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/principal-investigator.json",
            "https://vocabulary.raid.org/contributor.position.schema/307"
    );

    private static final Map<String, String> CONTRIBUTOR_POSITION_SCHEMA_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1/",
            "https://vocabulary.raid.org/contributor.position.schema/305"
    );

    private static final Map<String, String> DESCRIPTION_TYPE_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/alternative.json",
            "https://vocabulary.raid.org/description.type.schema/319",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/primary.json",
            "https://vocabulary.raid.org/description.type.schema/318"
    );

    private static final Map<String, String> DESCRIPTION_TYPE_SCHEMA_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/tree/main/scheme/description/type/v1/",
            "https://vocabulary.raid.org/description.type.schema/320"
    );

    private static final Map<String, String> ORGANISATION_ROLE_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/contractor.json",
            "https://vocabulary.raid.org/organisation.role.schema/185",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/lead-research-organisation.json",
            "https://vocabulary.raid.org/organisation.role.schema/182",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/other-organisation.json",
            "https://vocabulary.raid.org/organisation.role.schema/188",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/other-research-organisation.json",
            "https://vocabulary.raid.org/organisation.role.schema/183",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/partner-organisation.json",
            "https://vocabulary.raid.org/organisation.role.schema/184"
    );

    private static final Map<String, String> ORGANISATION_ROLE_SCHEMA_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/tree/main/scheme/organisation/role/v1/",
            "https://vocabulary.raid.org/organisation.role.schema/359"
    );

    private static final Map<String, String> RELATED_OBJECT_CATEGORY_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/input.json",
            "https://vocabulary.raid.org/relatedObject.category.id/191",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/internal.json",
            "https://vocabulary.raid.org/relatedObject.category.id/192",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/output.json",
            "https://vocabulary.raid.org/relatedObject.category.id/190"
    );

    private static final Map<String, String> RELATED_OBJECT_CATEGORY_SCHEMA_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/category/v1/",
            "https://vocabulary.raid.org/relatedObject.category.schemaUri/386"
    );

    private static final Map<String, String> RELATED_OBJECT_TYPE_MAP = new HashMap<>() {
        {
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/audiovisual.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/273");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/book-chapter.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/271");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/book.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/258");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/computational-notebook.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/256");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/conference-paper.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/264");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/conference-poster.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/248");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/conference-proceeding.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/262");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/data-paper.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/255");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/dataset.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/269");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/dissertation.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/253");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/educational-material.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/267");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/event.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/260");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/funding.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/272");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/image.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/257");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/instrument.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/266");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/journal-article.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/250");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/model.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/263");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/output-management-plan.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/247");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/physical-object.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/270");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/preprint.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/254");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/prize.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/268");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/report.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/252");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/service.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/274");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/software.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/259");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/sound.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/261");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/standard.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/251");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/text.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/265");
            put("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/workflow.json",
                    "https://vocabulary.raid.org/relatedObject.type.schema/249");
        }
    };

    private static Map<String, String> RELATED_OBJECT_TYPE_SCHEMA_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/type/v1/",
            "https://vocabulary.raid.org/relatedObject.type.schema/329"
    );

    private static final Map<String, String> RELATED_RAID_TYPE_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/has-part.json",
            "https://vocabulary.raid.org/relatedRaid.type.schema/201",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-continued-by.json",
            "https://vocabulary.raid.org/relatedRaid.type.schema/203",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-derived-from.json",
            "https://vocabulary.raid.org/relatedRaid.type.schema/200",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-identical-to.json",
            "https://vocabulary.raid.org/relatedRaid.type.schema/204",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-obsoleted-by.json",
            "https://vocabulary.raid.org/relatedRaid.type.schema/205",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-part-of.json",
            "https://vocabulary.raid.org/relatedRaid.type.schema/202",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-source-of.json",
            "https://vocabulary.raid.org/relatedRaid.type.schema/199",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/obsoletes.json",
            "https://vocabulary.raid.org/relatedRaid.type.schema/198"
    );

    private static final Map<String, String> RELATED_RAID_TYPE_SCHEMA_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/tree/main/scheme/related-raid/type/v1/",
            "https://vocabulary.raid.org/relatedRaid.type.schema/367"
    );

    private static final Map<String, String> TITLE_TYPE_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/alternative.json",
            "https://vocabulary.raid.org/title.type.schema/4",
            "https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/primary.json",
            "https://vocabulary.raid.org/title.type.schema/5"
    );

    private static final Map<String, String> TITLE_TYPE_SCHEMA_MAP = Map.of(
            "https://github.com/au-research/raid-metadata/tree/main/scheme/title/type/v1/",
            "https://vocabulary.raid.org/title.type.schema/376"
    );

    private final RaidRepository raidRepository;
    private final RaidHistoryService raidHistoryService;
    private final ServicePointRepository servicePointRepository;
    private final IdFactory idFactory;
    private final ObjectMapper objectMapper;
    private final JsonPatchFactory jsonPatchFactory;
    private final RaidHistoryRepository raidHistoryRepository;
    private final RaidHistoryRecordFactory raidHistoryRecordFactory;
    private final DataciteRequestFactory dataciteRequestFactory;
    private final RestTemplate restTemplate;
    private final DataciteProperties properties;
    private final HttpEntityFactory httpEntityFactory;
    private final RaidIngestService raidIngestService;
    private final RaidTitleRepository raidTitleRepository;
    private final RaidDescriptionRepository raidDescriptionRepository;
    private final RaidContributorRepository raidContributorRepository;
    private final RaidOrganisationRepository raidOrganisationRepository;
    private final RaidRelatedObjectRepository raidRelatedObjectRepository;
    private final RelatedRaidRepository relatedRaidRepository;

    // TODO: create datacite doi with suffix
    // TODO: if raid exists in raid_history table copy existing records into new DOI
    // TODO: invite contributor to authenticate

    public List<RaidDto> findAll() {
        return raidRepository
                .findAllByServicePointIdAndAccessTypeIdIn(20000003L, 1, 2, 3).stream()
                .map(record -> raidHistoryService.findByHandle(record.getHandle())
                        .orElseThrow(() -> new RuntimeException("Raid history not found: %s".formatted(record.getHandle())))
                )
                .toList();
    }

    @SneakyThrows
    public RaidDto upgrade(final RaidDto raidDto) {
        final var oldVersion = objectMapper.writeValueAsString(raidDto);

        upgradeAccess(raidDto);
        upgradeTitles(raidDto);
        if (raidDto.getDescription() != null) {
            upgradeDescriptions(raidDto);
        }
        if (raidDto.getContributor() != null) {
            upgradeContributors(raidDto);
        }
        if (raidDto.getOrganisation() != null) {
            upgradeOrganisations(raidDto);
        }
        if (raidDto.getRelatedObject() != null) {
            upgradeRelatedObjects(raidDto);
        }
        if (raidDto.getRelatedRaid() != null) {
            upgradeRelatedRaids(raidDto);
        }

        final var id = raidDto.getIdentifier().getId();
        final var oldHandle = id.substring(id.indexOf('/', 8) + 1);
        final var servicePointId = raidDto.getIdentifier().getOwner().getServicePoint();

        final var servicePoint = servicePointRepository.findById(servicePointId)
                .orElseThrow(() -> new RuntimeException("No such service point %d".formatted(servicePointId)));

        final var suffix = id.substring(id.lastIndexOf('/') + 1);
        final var handle = new Handle(servicePoint.getPrefix(), suffix);
        final var version = raidDto.getIdentifier().getVersion();

        final var identifier = idFactory.create(handle.toString(), servicePoint);
        identifier.version(version + 1);

        raidDto.identifier(identifier);

        //TODO: create datacite record
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

        //TODO: get existing history for raid and assign it to new id
        final var existingHistory = raidHistoryRepository.findAllByHandle(oldHandle);

        existingHistory.forEach(record -> {
            record.setHandle(handle.toString());
            raidHistoryRepository.insert(record);
        });

        final var raidString = objectMapper.writeValueAsString(raidDto);

        final var diff = jsonPatchFactory.create(oldVersion, raidString);
        raidHistoryRepository.insert(raidHistoryRecordFactory.create(handle, raidDto.getIdentifier().getVersion(), ChangeType.PATCH, diff));

        raidIngestService.create(raidDto);

        //TODO: update related raid handles
        relatedRaidRepository.updateAllByRelatedHandle(oldHandle, identifier.getId());

        raidTitleRepository.deleteAllByHandle(oldHandle);
        raidDescriptionRepository.deleteAllByHandle(oldHandle);
        raidContributorRepository.deleteAllByHandle(oldHandle);
        raidOrganisationRepository.deleteAllByHandle(oldHandle);
        raidRelatedObjectRepository.deleteAllByHandle(oldHandle);
        relatedRaidRepository.deleteAllByHandle(oldHandle);
        raidRepository.deleteByHandle(oldHandle);

        return raidDto;
    }

    private void upgradeRelatedRaids(final RaidDto raidDto) {
        final var relatedRaids = raidDto.getRelatedRaid().stream()
                .map(relatedRaid -> new RelatedRaid()
                        .id(relatedRaid.getId())
                        .type(new RelatedRaidType()
                                .id(RELATED_RAID_TYPE_MAP.get(relatedRaid.getType().getId()))
                                .schemaUri(RELATED_RAID_TYPE_SCHEMA_MAP.get(relatedRaid.getType().getSchemaUri()))
                        )
                )
                .toList();

        raidDto.relatedRaid(relatedRaids);
    }

    private void upgradeRelatedObjects(final RaidDto raidDto) {
        final var relatedObjects = raidDto.getRelatedObject().stream()
                .map(relatedObject -> new RelatedObject()
                        .id(relatedObject.getId())
                        .schemaUri(relatedObject.getSchemaUri())
                        .category(relatedObject.getCategory().stream()
                                .map(category -> new RelatedObjectCategory()
                                        .id(RELATED_OBJECT_CATEGORY_MAP.get(category.getId()))
                                        .schemaUri(RELATED_OBJECT_CATEGORY_SCHEMA_MAP.get(category.getSchemaUri()))
                                )
                                .toList()
                        )
                        .type(new RelatedObjectType()
                                .id(RELATED_OBJECT_TYPE_MAP.get(relatedObject.getType().getId()))
                                .schemaUri(RELATED_OBJECT_TYPE_SCHEMA_MAP.get(relatedObject.getType().getSchemaUri()))
                        )
                )
                .toList();

        raidDto.relatedObject(relatedObjects);
    }

    private void upgradeOrganisations(final RaidDto raidDto) {
        final var organisations = raidDto.getOrganisation().stream()
                .map(organisation -> new Organisation()
                        .id(organisation.getId())
                        .schemaUri(organisation.getSchemaUri())
                        .role(organisation.getRole().stream()
                                .map(organisationRole -> new OrganisationRole()
                                        .id(ORGANISATION_ROLE_MAP.get(organisationRole.getId()))
                                        .schemaUri(ORGANISATION_ROLE_SCHEMA_MAP.get(organisationRole.getSchemaUri()))
                                        .startDate(organisationRole.getStartDate())
                                        .endDate(organisationRole.getEndDate())
                                )
                                .toList()
                        )
                )
                .toList();

        raidDto.organisation(organisations);
    }


    private void upgradeContributors(final RaidDto raidDto) {
        final var contributors = raidDto.getContributor().stream()
                .map(contributor -> new Contributor()
                        .id(contributor.getId())
                        .schemaUri(contributor.getSchemaUri())
                        .leader(contributor.getPosition().stream()
                                .anyMatch(contributorPosition -> contributorPosition.getId().equals("https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/leader.json")))
                        .contact(contributor.getPosition().stream()
                                .anyMatch(contributorPosition -> contributorPosition.getId().equals("https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/contact-person.json")))
                        .position(contributor.getPosition().stream()
                                .filter(contributorPosition -> !contributorPosition.getId().equals(LEAD_CONTRIBUTOR_POSITION_ID))
                                .filter(contributorPosition -> !contributorPosition.getId().equals(CONTACT_CONTRIBUTOR_POSITION_ID))
                                .map(contributorPosition -> new ContributorPosition()
                                        .id(CONTRIBUTOR_POSITION_MAP.get(contributorPosition.getId()))
                                        .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_MAP.get(contributorPosition.getSchemaUri()))
                                        .startDate(contributorPosition.getStartDate())
                                        .endDate(contributorPosition.getEndDate())
                                )
                                .toList()
                        )
                        .role(contributor.getRole())
                )
                .toList();

        raidDto.contributor(contributors);
    }


    private void upgradeDescriptions(final RaidDto raidDto) {
        final var descriptions = raidDto.getDescription().stream()
                .map(description -> new Description()
                        .text(description.getText())
                        .language(description.getLanguage())
                        .type(new DescriptionType()
                                .id(DESCRIPTION_TYPE_MAP.get(description.getType().getId()))
                                .schemaUri(DESCRIPTION_TYPE_SCHEMA_MAP.get(description.getType().getSchemaUri()))
                        )
                )
                .toList();

        raidDto.description(descriptions);
    }

    private void upgradeTitles(final RaidDto raidDto) {
        final var titles = raidDto.getTitle().stream()
                .map(title -> new Title()
                        .text(title.getText())
                        .startDate(title.getStartDate())
                        .endDate(title.getEndDate())
                        .language(title.getLanguage())
                        .type(new TitleType()
                                .id(TITLE_TYPE_MAP.get(title.getType().getId()))
                                .schemaUri(TITLE_TYPE_SCHEMA_MAP.get(title.getType().getSchemaUri()))
                        )
                )
                .toList();

        raidDto.title(titles);
    }


    private void upgradeAccess(final RaidDto raidDto) {
        final var closedRaid = raidDto.getAccess().getType().getId().equals("https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json");

        final var statement = closedRaid ? "Access set to embargoed from closed as part of upgrade process on %s"
                .formatted(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) : raidDto.getAccess().getStatement().getText();

        final var embargoExpiry = closedRaid ? LocalDate.now().plusMonths(18) : raidDto.getAccess().getEmbargoExpiry();

        final var access = new Access()
                .statement(new AccessStatement()
                        .text(statement)
                        .language(new Language()
                                .id("eng")
                                .schemaUri(SchemaValues.LANGUAGE_SCHEMA.getUri())
                        )
                )
                .type(new AccessType()
                        .schemaUri(ACCESS_TYPE_SCHEMA_MAP.get(raidDto.getAccess().getType().getSchemaUri()))
                        .id(ACCESS_TYPE_MAP.get(raidDto.getAccess().getType().getId()))
                )
                .embargoExpiry(embargoExpiry);

        raidDto.access(access);
    }









}
