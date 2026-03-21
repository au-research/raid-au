package au.org.raid.api.factory.datacite;

import au.org.raid.api.config.properties.IdentifierProperties;
import au.org.raid.api.model.datacite.doi.*;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.idl.raidv2.model.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
@RequiredArgsConstructor
public class DataciteAttributesDtoFactory {
    private final DataciteTitleFactory titleFactory;
    private final DataciteCreatorFactory creatorFactory;
    private final DataciteDateFactory dateFactory;
    private final DataciteContributorFactory contributorFactory;
    private final DataciteDescriptionFactory descriptionFactory;
    private final DataciteRelatedIdentifierFactory relatedIdentifierFactory;
    private final DataciteRightFactory dataciteRightFactory;
    private final DataciteTypesFactory typesFactory;
    private final DataciteAlternateIdentifierFactory alternateIdentifierFactory;
    private final DatacitePublisherFactory publisherFactory;
    private final DataciteFundingReferenceFactory fundingReferenceFactory;
    private final IdentifierProperties identifierProperties;

    private List<DataciteCreator> addCreators(final List<Contributor> contributors) {

        final var creators = contributors.stream()
                .map(creatorFactory::create)
                .filter(Objects::nonNull)
                .filter(creator -> !isBlank(creator.getName()))
                .toList();

        return creators.isEmpty() ? List.of(new DataciteCreator()) : creators;

    }

    @SneakyThrows
    public DataciteAttributesDto create(RaidCreateRequest request, String handle) {
        final var url = identifierProperties.getLandingPrefix() + handle;

        final var event = request.getAccess().getType().getId().equals(SchemaValues.ACCESS_TYPE_OPEN.getUri()) ?
                "publish" : "register";

        final var contributors = new ArrayList<DataciteContributor>();

        contributors.add(contributorFactory.create(
                request.getIdentifier().getRegistrationAgency()
        ));

        final var fundingReferences = new ArrayList<DataciteFundingReference>();

        if (request.getOrganisation() != null) {
            contributors.addAll(request.getOrganisation().stream()
                            .filter(organisation -> !organisation.getRole().stream()
                                    .filter(r -> !r.getId().equals(SchemaValues.FUNDER_ORGANISATION_ROLE.getUri()))
                                    .toList()
                                    .isEmpty()// Any organisation that contains a role that is not 'Funder'
                            )
                    .map(contributorFactory::create)
                    .toList());

            fundingReferences.addAll(request.getOrganisation().stream()
                    .filter(organisation -> organisation.getRole().stream()
                            .map(OrganisationRole::getId)
                            .toList()
                            .contains(SchemaValues.FUNDER_ORGANISATION_ROLE.getUri())

                    )
                    .map(fundingReferenceFactory::create)
                    .toList());
        }

        final var publisher = publisherFactory.create(request.getIdentifier().getOwner());

        final var dates = List.of(dateFactory.create(request.getDate()));

        final var titles = new ArrayList<DataciteTitle>();

        final var primaryTitle = request.getTitle().stream()
                .filter(t -> t.getType().getId().equals(SchemaValues.PRIMARY_TITLE_TYPE.getUri()))
                .findFirst()
                .orElseThrow();

        titles.add(titleFactory.create(primaryTitle));

        titles.addAll(request.getTitle().stream()
                .filter(t -> !t.getType().getId().equals(SchemaValues.PRIMARY_TITLE_TYPE.getUri()))
                .map(titleFactory::create)
                .toList());

        final var descriptions = new ArrayList<DataciteDescription>();

        if (request.getDescription() != null) {
            descriptions.addAll(request.getDescription().stream()
                    .map(descriptionFactory::create)
                    .toList());
        }

        final var creators = addCreators(request.getContributor());

        final var relatedIdentifiers = new ArrayList<DataciteRelatedIdentifier>();

        if (request.getRelatedObject() != null) {
            relatedIdentifiers.addAll(request.getRelatedObject().stream()
                            .filter(relatedObject -> !(relatedObject.getCategory().stream()
                                    .map(RelatedObjectCategory::getId)
                                    .toList()
                                    .contains(SchemaValues.INPUT_RELATED_OBJECT_CATEGORY.getUri())
                                    &&
                                    relatedObject.getType().getId().equals(SchemaValues.FUNDING_OBJECT_TYPE.getUri()))
                            )
                    .map(relatedIdentifierFactory::create)
                    .toList());
        }


        if (request.getAlternateUrl() != null) {
            relatedIdentifiers.addAll(request.getAlternateUrl().stream()
                    .map(relatedIdentifierFactory::create)
                    .toList());
        }

        if (request.getRelatedRaid() != null) {
            relatedIdentifiers.addAll(request.getRelatedRaid().stream()
                    .map(relatedIdentifierFactory::create)
                    .toList());
        }

        final var alternateIdentifiers = new ArrayList<DataciteAlternateIdentifier>();
        alternateIdentifiers.add(alternateIdentifierFactory.create(request.getIdentifier()));

        if (request.getAlternateIdentifier() != null) {
            alternateIdentifiers.addAll(request.getAlternateIdentifier().stream()
                    .map(alternateIdentifierFactory::create)
                    .toList());
        }

        final var dataciteTypes = typesFactory.create();

        final var prefix = handle.split("/")[0];

        return new DataciteAttributesDto()
                .setPrefix(prefix)
                .setDoi(handle)
                .setPublisher(publisher) // $.identifier.registrationAgency
                .setPublicationYear(String.valueOf(java.time.Year.now())) // TODO: year of start date
                .setTypes(dataciteTypes)
                .setTitles(titles)
                .setCreators(creators)
                .setDates(dates)
                .setContributors(contributors)
                .setDescriptions(descriptions)
                .setRelatedIdentifiers(relatedIdentifiers)
                .setAlternateIdentifiers(alternateIdentifiers)
                .setFundingReferences(fundingReferences)
                .setEvent(event)
                .setUrl(url);
    }

    @SneakyThrows
    public DataciteAttributesDto create(RaidUpdateRequest request, String handle) {
        final var url = identifierProperties.getLandingPrefix() + handle;

        final var event = request.getAccess().getType().getId().equals(SchemaValues.ACCESS_TYPE_OPEN.getUri()) ?
                "publish" : "register";
        final var contributors = new ArrayList<DataciteContributor>();

        contributors.add(contributorFactory.create(
                request.getIdentifier().getRegistrationAgency()
        ));

        final var fundingReferences = new ArrayList<DataciteFundingReference>();

        if (request.getOrganisation() != null) {
            contributors.addAll(request.getOrganisation().stream()
                    .filter(organisation -> !organisation.getRole().stream()
                            .filter(r -> !r.getId().equals(SchemaValues.FUNDER_ORGANISATION_ROLE.getUri()))
                            .toList()
                            .isEmpty()// Any organisation that contains a role that is not 'Funder'
                    )
                    .map(contributorFactory::create)
                    .toList());

            fundingReferences.addAll(request.getOrganisation().stream()
                    .filter(organisation -> organisation.getRole().stream()
                            .map(OrganisationRole::getId)
                            .toList()
                            .contains(SchemaValues.FUNDER_ORGANISATION_ROLE.getUri())

                    )
                    .map(fundingReferenceFactory::create)
                    .toList());
        }

        final var publisher = publisherFactory.create(request.getIdentifier().getOwner());

        final var dates = List.of(dateFactory.create(request.getDate()));

        final var titles = new ArrayList<DataciteTitle>();

        final var primaryTitle = request.getTitle().stream()
                .filter(t -> t.getType().getId().equals(SchemaValues.PRIMARY_TITLE_TYPE.getUri()))
                .findFirst()
                .orElseThrow();

        titles.add(titleFactory.create(primaryTitle));

        titles.addAll(request.getTitle().stream()
                .filter(t -> !t.getType().getId().equals(SchemaValues.PRIMARY_TITLE_TYPE.getUri()))
                .map(titleFactory::create)
                .toList());

        final var descriptions = new ArrayList<DataciteDescription>();

        if (request.getDescription() != null) {
            descriptions.addAll(request.getDescription().stream()
                    .map(descriptionFactory::create)
                    .toList());
        }

        final var creators = addCreators(request.getContributor());

        final var relatedIdentifiers = new ArrayList<DataciteRelatedIdentifier>();

        if (request.getRelatedObject() != null) {
            relatedIdentifiers.addAll(request.getRelatedObject().stream()
                    .filter(relatedObject -> !(relatedObject.getCategory().stream()
                            .map(RelatedObjectCategory::getId)
                            .toList()
                            .contains(SchemaValues.INPUT_RELATED_OBJECT_CATEGORY.getUri())
                            &&
                            relatedObject.getType().getId().equals(SchemaValues.FUNDING_OBJECT_TYPE.getUri()))
                    )
                    .map(relatedIdentifierFactory::create)
                    .toList());
        }

        if (request.getAlternateUrl() != null) {
            relatedIdentifiers.addAll(request.getAlternateUrl().stream()
                    .map(relatedIdentifierFactory::create)
                    .toList());
        }

        if (request.getRelatedRaid() != null) {
            relatedIdentifiers.addAll(request.getRelatedRaid().stream()
                    .map(relatedIdentifierFactory::create)
                    .toList());
        }

        final var alternateIdentifiers = new ArrayList<DataciteAlternateIdentifier>();
        alternateIdentifiers.add(alternateIdentifierFactory.create(request.getIdentifier()));

        if (request.getAlternateIdentifier() != null) {
            alternateIdentifiers.addAll(request.getAlternateIdentifier().stream()
                    .map(alternateIdentifierFactory::create)
                    .toList());
        }

        final var dataciteTypes = typesFactory.create();

        final var prefix = handle.split("/")[0];

        return new DataciteAttributesDto()
                .setPrefix(prefix)
                .setDoi(handle)
                .setPublisher(publisher) // $.identifier.registrationAgency
                .setPublicationYear(String.valueOf(java.time.Year.now())) // TODO: year of start date
                .setTypes(dataciteTypes)
                .setTitles(titles)
                .setCreators(creators)
                .setDates(dates)
                .setContributors(contributors)
                .setDescriptions(descriptions)
                .setRelatedIdentifiers(relatedIdentifiers)
                .setAlternateIdentifiers(alternateIdentifiers)
                .setFundingReferences(fundingReferences)
                .setUrl(url)
                .setEvent(event);
    }


    @SneakyThrows
    public DataciteAttributesDto create(RaidDto request, String handle) {
        final var url = identifierProperties.getLandingPrefix() + handle;

        final var event = request.getAccess().getType().getId().equals(SchemaValues.ACCESS_TYPE_OPEN.getUri()) ?
                "publish" : "register";
        final var contributors = new ArrayList<DataciteContributor>();

        contributors.add(contributorFactory.create(
                request.getIdentifier().getRegistrationAgency()
        ));

        final var fundingReferences = new ArrayList<DataciteFundingReference>();

        if (request.getOrganisation() != null) {
            contributors.addAll(request.getOrganisation().stream()
                    .filter(organisation -> !organisation.getRole().stream()
                            .filter(r -> !r.getId().equals(SchemaValues.FUNDER_ORGANISATION_ROLE.getUri()))
                            .toList()
                            .isEmpty()// Any organisation that contains a role that is not 'Funder'
                    )
                    .map(contributorFactory::create)
                    .toList());

            fundingReferences.addAll(request.getOrganisation().stream()
                    .filter(organisation -> organisation.getRole().stream()
                            .map(OrganisationRole::getId)
                            .toList()
                            .contains(SchemaValues.FUNDER_ORGANISATION_ROLE.getUri())

                    )
                    .map(fundingReferenceFactory::create)
                    .toList());
        }

        final var publisher = publisherFactory.create(request.getIdentifier().getOwner());

        final var dates = List.of(dateFactory.create(request.getDate()));

        final var titles = new ArrayList<DataciteTitle>();

        final var primaryTitle = request.getTitle().stream()
                .filter(t -> t.getType().getId().equals(SchemaValues.PRIMARY_TITLE_TYPE.getUri()))
                .findFirst()
                .orElseThrow();

        titles.add(titleFactory.create(primaryTitle));

        titles.addAll(request.getTitle().stream()
                .filter(t -> !t.getType().getId().equals(SchemaValues.PRIMARY_TITLE_TYPE.getUri()))
                .map(titleFactory::create)
                .toList());

        final var descriptions = new ArrayList<DataciteDescription>();

        if (request.getDescription() != null) {
            descriptions.addAll(request.getDescription().stream()
                    .map(descriptionFactory::create)
                    .toList());
        }

        final var creators = (request.getContributor() != null) ?
                addCreators(request.getContributor()) : new ArrayList<DataciteCreator>();

        final var relatedIdentifiers = new ArrayList<DataciteRelatedIdentifier>();

        if (request.getRelatedObject() != null) {
            relatedIdentifiers.addAll(request.getRelatedObject().stream()
                    .filter(relatedObject -> !(relatedObject.getCategory().stream()
                            .map(RelatedObjectCategory::getId)
                            .toList()
                            .contains(SchemaValues.INPUT_RELATED_OBJECT_CATEGORY.getUri())
                            &&
                            relatedObject.getType().getId().equals(SchemaValues.FUNDING_OBJECT_TYPE.getUri()))
                    )
                    .map(relatedIdentifierFactory::create)
                    .toList());
        }

        if (request.getAlternateUrl() != null) {
            relatedIdentifiers.addAll(request.getAlternateUrl().stream()
                    .map(relatedIdentifierFactory::create)
                    .toList());
        }

        if (request.getRelatedRaid() != null) {
            relatedIdentifiers.addAll(request.getRelatedRaid().stream()
                    .map(relatedIdentifierFactory::create)
                    .toList());
        }

        final var alternateIdentifiers = new ArrayList<DataciteAlternateIdentifier>();
        alternateIdentifiers.add(alternateIdentifierFactory.create(request.getIdentifier()));

        if (request.getAlternateIdentifier() != null) {
            alternateIdentifiers.addAll(request.getAlternateIdentifier().stream()
                    .map(alternateIdentifierFactory::create)
                    .toList());
        }

        final var dataciteTypes = typesFactory.create();

        final var prefix = handle.split("/")[0];

        return new DataciteAttributesDto()
                .setPrefix(prefix)
                .setDoi(handle)
                .setPublisher(publisher) // $.identifier.registrationAgency
                .setPublicationYear(String.valueOf(java.time.Year.now())) // TODO: year of start date
                .setTypes(dataciteTypes)
                .setTitles(titles)
                .setCreators(creators)
                .setDates(dates)
                .setContributors(contributors)
                .setDescriptions(descriptions)
                .setRelatedIdentifiers(relatedIdentifiers)
                .setAlternateIdentifiers(alternateIdentifiers)
                .setFundingReferences(fundingReferences)
                .setUrl(url)
                .setEvent(event);
    }
}
