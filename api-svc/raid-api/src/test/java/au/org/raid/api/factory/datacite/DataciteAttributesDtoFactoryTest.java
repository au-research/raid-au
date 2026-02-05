package au.org.raid.api.factory.datacite;

import au.org.raid.api.config.properties.IdentifierProperties;
import au.org.raid.api.model.datacite.doi.*;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataciteAttributesDtoFactoryTest {
    @Mock
    private DataciteContributorFactory contributorFactory;
    @Mock
    private DatacitePublisherFactory publisherFactory;
    @Mock
    private DataciteTitleFactory titleFactory;
    @Mock
    private DataciteDateFactory dateFactory;
    @Mock
    private DataciteTypesFactory typesFactory;
    @Mock
    private DataciteDescriptionFactory descriptionFactory;
    @Mock
    private DataciteCreatorFactory creatorFactory;
    @Mock
    private DataciteFundingReferenceFactory fundingReferenceFactory;
    @Mock
    private DataciteRelatedIdentifierFactory relatedIdentifierFactory;
    @Mock
    private DataciteAlternateIdentifierFactory alternateIdentifierFactory;

    @Mock
    private IdentifierProperties identifierProperties;

    @InjectMocks
    private DataciteAttributesDtoFactory attributesDtoFactory;

    @Test
    @DisplayName("Contributors are added on create")
    void setContributorsOnCreate_AwaitingAuthentication() {
        final var handle = "_handle";
        final var registrationAgencyId = "registration-agency-id";
        final var registrationAgencySchemaUri = "registration-agency-schema-uri";
        final var organisationSchemaUri = "organisation-schema-uri";
        final var organisationId = "organisation-id";
        final var funderId = "funder-id";
        final var ownerId = "owner-id";
        final var ownerSchemaUri = "owner-schema-uri";
        final var primaryTitleText = "primary-title";
        final var alternativeTitleText = "alternative-title";
        final var languageId = "eng";
        final var raidAgencyUrl = "raid-agency-url";

        final var primaryTitle = new Title()
                .text(primaryTitleText)
                .type(new TitleType()
                        .id(SchemaValues.PRIMARY_TITLE_TYPE.getUri()))
                .language(new Language().id(languageId));

        final var alternativeTitle = new Title()
                .text(alternativeTitleText)
                .type(new TitleType()
                        .id(SchemaValues.ALTERNATIVE_TITLE_TYPE.getUri()));

        final var owner = new Owner()
                .id(ownerId)
                .schemaUri(ownerSchemaUri);

        final var registrationAgency = new RegistrationAgency()
                .id(registrationAgencyId)
                .schemaUri(registrationAgencySchemaUri);

        final var organisation = new Organisation()
                .id(organisationId)
                .schemaUri(organisationSchemaUri)
                .role(List.of(new OrganisationRole().id(SchemaValues.LEAD_RESEARCH_ORGANISATION_ROLE.getUri())));

        final var funder = new Organisation()
                .id(funderId)
                .schemaUri(organisationSchemaUri)
                .role(List.of(new OrganisationRole().id(SchemaValues.FUNDER_ORGANISATION_ROLE.getUri())));

        final var date = new Date().startDate("2020");

        final var description = new Description();
        final var contributor = new Contributor()
                .status("AWAITING_AUTHENTICATION");

        final var relatedObject = new RelatedObject()
                .type(new RelatedObjectType().id(SchemaValues.BOOK_OBJECT_TYPE.getUri()))
                .category(List.of(new RelatedObjectCategory().id(SchemaValues.INPUT_RELATED_OBJECT_CATEGORY.getUri())));
        final var fundingRelatedObject = new RelatedObject()
                .type(new RelatedObjectType().id(SchemaValues.FUNDING_OBJECT_TYPE.getUri()))
                .category(List.of(new RelatedObjectCategory().id(SchemaValues.INPUT_RELATED_OBJECT_CATEGORY.getUri())));
        final var alternateIdentifier = new AlternateIdentifier();
        final var relatedRaid = new RelatedRaid();
        final var access = new Access()
                .type(new AccessType().id(SchemaValues.ACCESS_TYPE_OPEN.getUri()));

        final var request = new RaidCreateRequest()
                .identifier(new Id()
                        .registrationAgency(registrationAgency)
                        .owner(owner)
                        .raidAgencyUrl(raidAgencyUrl)
                )
                .organisation(List.of(organisation, funder))
                .title(List.of(alternativeTitle, primaryTitle))
                .date(date)
                .description(List.of(description))
                .contributor(List.of(contributor))
                .relatedObject(List.of(fundingRelatedObject, relatedObject))
                .alternateIdentifier(List.of(alternateIdentifier))
                .relatedRaid(List.of(relatedRaid))
                .access(access);

        final var registrationAgencyContributor = new DataciteContributor()
                .setContributorType("RegistrationAgency");
        final var organisationContributor = new DataciteContributor().setContributorType("Other");
        final var publisher = new DatacitePublisher();
        final var dataciteDate = new DataciteDate();
        final var primaryDataciteTitle = new DataciteTitle()
                .setTitle(primaryTitleText)
                .setLang(languageId);
        final var alternativeDataciteTitle = new DataciteTitle()
                .setTitle(alternativeTitleText)
                .setTitleType("Other");
        final var dataciteDescription = new DataciteDescription();
        final var creator = new DataciteCreator();
        final var fundingReference = new DataciteFundingReference();
        final var relatedIdentifier = new DataciteRelatedIdentifier();
        final var dataciteAlternateIdentifier = new DataciteAlternateIdentifier();
        final var dataciteRaidAgencyUrl = new DataciteAlternateIdentifier().setAlternateIdentifier(raidAgencyUrl).setAlternateIdentifierType("RaidAgencyUrl");
        final var relatedRaidIdentifier = new DataciteRelatedIdentifier();
        final var types = new DataciteTypes();
        final var landingPrefix = "landing-prefix/";

        when(titleFactory.create(primaryTitle)).thenReturn(primaryDataciteTitle);
        when(titleFactory.create(alternativeTitle)).thenReturn(alternativeDataciteTitle);

        when(contributorFactory.create(registrationAgency)).thenReturn(registrationAgencyContributor);
        when(contributorFactory.create(organisation)).thenReturn(organisationContributor);
        when(dateFactory.create(date)).thenReturn(dataciteDate);
        when(publisherFactory.create(owner)).thenReturn(publisher);
        when(descriptionFactory.create(description)).thenReturn(dataciteDescription);
//        when(creatorFactory.create(contributor)).thenReturn(creator);
        when(fundingReferenceFactory.create(funder)).thenReturn(fundingReference);
        when(relatedIdentifierFactory.create(relatedObject)).thenReturn(relatedIdentifier);
        when(alternateIdentifierFactory.create(alternateIdentifier)).thenReturn(dataciteAlternateIdentifier);
        when(alternateIdentifierFactory.create(request.getIdentifier())).thenReturn(dataciteRaidAgencyUrl);
        when(relatedIdentifierFactory.create(relatedRaid)).thenReturn(relatedRaidIdentifier);
        when(typesFactory.create()).thenReturn(types);
        when(identifierProperties.getLandingPrefix()).thenReturn(landingPrefix);

        final var result = attributesDtoFactory.create(request, handle);

//        verifyNoInteractions(creatorFactory);

        assertThat(result.getContributors(), is(List.of(registrationAgencyContributor, organisationContributor)));
        assertThat(result.getPublisher(), is(publisher));
        assertThat(result.getDates(), is(List.of(dataciteDate)));
        assertThat(result.getTitles(), is(List.of(primaryDataciteTitle, alternativeDataciteTitle)));
        assertThat(result.getDescriptions(), is(List.of(dataciteDescription)));
        assertThat(result.getCreators(), is(List.of(new DataciteCreator())));
        assertThat(result.getFundingReferences(), is(List.of(fundingReference)));
        assertThat(result.getRelatedIdentifiers(), is(List.of(relatedIdentifier, relatedRaidIdentifier)));
        assertThat(result.getAlternateIdentifiers(), is(List.of(dataciteRaidAgencyUrl, dataciteAlternateIdentifier)));
        assertThat(result.getTypes(), is(types));
        assertThat(result.getUrl(), is(landingPrefix + handle));
        assertThat(result.getEvent(), is("publish"));
    }

    @Test
    @DisplayName("Contributors are added on create")
    void setContributorsOnCreate_Authenticated() {
        final var handle = "_handle";
        final var registrationAgencyId = "registration-agency-id";
        final var registrationAgencySchemaUri = "registration-agency-schema-uri";
        final var organisationSchemaUri = "organisation-schema-uri";
        final var organisationId = "organisation-id";
        final var funderId = "funder-id";
        final var ownerId = "owner-id";
        final var ownerSchemaUri = "owner-schema-uri";
        final var primaryTitleText = "primary-title";
        final var alternativeTitleText = "alternative-title";
        final var languageId = "eng";
        final var raidAgencyUrl = "raid-agency-url";

        final var primaryTitle = new Title()
                .text(primaryTitleText)
                .type(new TitleType()
                        .id(SchemaValues.PRIMARY_TITLE_TYPE.getUri()))
                .language(new Language().id(languageId));

        final var alternativeTitle = new Title()
                .text(alternativeTitleText)
                .type(new TitleType()
                        .id(SchemaValues.ALTERNATIVE_TITLE_TYPE.getUri()));

        final var owner = new Owner()
                .id(ownerId)
                .schemaUri(ownerSchemaUri);

        final var registrationAgency = new RegistrationAgency()
                .id(registrationAgencyId)
                .schemaUri(registrationAgencySchemaUri);

        final var organisation = new Organisation()
                .id(organisationId)
                .schemaUri(organisationSchemaUri)
                .role(List.of(new OrganisationRole().id(SchemaValues.LEAD_RESEARCH_ORGANISATION_ROLE.getUri())));

        final var funder = new Organisation()
                .id(funderId)
                .schemaUri(organisationSchemaUri)
                .role(List.of(new OrganisationRole().id(SchemaValues.FUNDER_ORGANISATION_ROLE.getUri())));

        final var date = new Date().startDate("2020");

        final var description = new Description();
        final var contributor = new Contributor()
                .status("AUTHENTICATED");

        final var relatedObject = new RelatedObject()
                .type(new RelatedObjectType().id(SchemaValues.BOOK_OBJECT_TYPE.getUri()))
                .category(List.of(new RelatedObjectCategory().id(SchemaValues.INPUT_RELATED_OBJECT_CATEGORY.getUri())));
        final var fundingRelatedObject = new RelatedObject()
                .type(new RelatedObjectType().id(SchemaValues.FUNDING_OBJECT_TYPE.getUri()))
                .category(List.of(new RelatedObjectCategory().id(SchemaValues.INPUT_RELATED_OBJECT_CATEGORY.getUri())));
        final var alternateIdentifier = new AlternateIdentifier();
        final var relatedRaid = new RelatedRaid();
        final var access = new Access()
                .type(new AccessType().id(SchemaValues.ACCESS_TYPE_OPEN.getUri()));

        final var request = new RaidCreateRequest()
                .identifier(new Id()
                        .registrationAgency(registrationAgency)
                        .owner(owner)
                        .raidAgencyUrl(raidAgencyUrl)
                )
                .organisation(List.of(organisation, funder))
                .title(List.of(alternativeTitle, primaryTitle))
                .date(date)
                .description(List.of(description))
                .contributor(List.of(contributor))
                .relatedObject(List.of(fundingRelatedObject, relatedObject))
                .alternateIdentifier(List.of(alternateIdentifier))
                .relatedRaid(List.of(relatedRaid))
                .access(access);

        final var registrationAgencyContributor = new DataciteContributor()
                .setContributorType("RegistrationAgency");
        final var organisationContributor = new DataciteContributor().setContributorType("Other");
        final var publisher = new DatacitePublisher();
        final var dataciteDate = new DataciteDate();
        final var primaryDataciteTitle = new DataciteTitle()
                .setTitle(primaryTitleText)
                .setLang(languageId);
        final var alternativeDataciteTitle = new DataciteTitle()
                .setTitle(alternativeTitleText)
                .setTitleType("Other");
        final var dataciteDescription = new DataciteDescription();
        final var creator = new DataciteCreator();
        final var fundingReference = new DataciteFundingReference();
        final var relatedIdentifier = new DataciteRelatedIdentifier();
        final var dataciteAlternateIdentifier = new DataciteAlternateIdentifier();
        final var dataciteRaidAgencyUrl = new DataciteAlternateIdentifier().setAlternateIdentifier(raidAgencyUrl).setAlternateIdentifierType("RaidAgencyUrl");
        final var relatedRaidIdentifier = new DataciteRelatedIdentifier();
        final var types = new DataciteTypes();
        final var landingPrefix = "landing-prefix/";

        when(titleFactory.create(primaryTitle)).thenReturn(primaryDataciteTitle);
        when(titleFactory.create(alternativeTitle)).thenReturn(alternativeDataciteTitle);

        when(contributorFactory.create(registrationAgency)).thenReturn(registrationAgencyContributor);
        when(contributorFactory.create(organisation)).thenReturn(organisationContributor);
        when(dateFactory.create(date)).thenReturn(dataciteDate);
        when(publisherFactory.create(owner)).thenReturn(publisher);
        when(descriptionFactory.create(description)).thenReturn(dataciteDescription);
        when(creatorFactory.create(contributor)).thenReturn(creator);
        when(fundingReferenceFactory.create(funder)).thenReturn(fundingReference);
        when(relatedIdentifierFactory.create(relatedObject)).thenReturn(relatedIdentifier);
        when(alternateIdentifierFactory.create(alternateIdentifier)).thenReturn(dataciteAlternateIdentifier);
        when(alternateIdentifierFactory.create(request.getIdentifier())).thenReturn(dataciteRaidAgencyUrl);
        when(relatedIdentifierFactory.create(relatedRaid)).thenReturn(relatedRaidIdentifier);
        when(typesFactory.create()).thenReturn(types);
        when(identifierProperties.getLandingPrefix()).thenReturn(landingPrefix);

        final var result = attributesDtoFactory.create(request, handle);

//        verifyNoInteractions(creatorFactory);
        assertThat(result.getContributors(), is(List.of(registrationAgencyContributor, organisationContributor)));
        assertThat(result.getPublisher(), is(publisher));
        assertThat(result.getDates(), is(List.of(dataciteDate)));
        assertThat(result.getTitles(), is(List.of(primaryDataciteTitle, alternativeDataciteTitle)));
        assertThat(result.getDescriptions(), is(List.of(dataciteDescription)));
        assertThat(result.getCreators(), is(List.of(creator)));
        assertThat(result.getFundingReferences(), is(List.of(fundingReference)));
        assertThat(result.getRelatedIdentifiers(), is(List.of(relatedIdentifier, relatedRaidIdentifier)));
        assertThat(result.getAlternateIdentifiers(), is(List.of(dataciteRaidAgencyUrl, dataciteAlternateIdentifier)));
        assertThat(result.getTypes(), is(types));
        assertThat(result.getUrl(), is(landingPrefix + handle));
        assertThat(result.getEvent(), is("publish"));
    }


    @Test
    @DisplayName("Set event to 'register' if embargoed")
    void embargoedDoesNotPublish() {
        final var handle = "_handle";
        final var registrationAgencyId = "registration-agency-id";
        final var registrationAgencySchemaUri = "registration-agency-schema-uri";
        final var organisationSchemaUri = "organisation-schema-uri";
        final var organisationId = "organisation-id";
        final var funderId = "funder-id";
        final var ownerId = "owner-id";
        final var ownerSchemaUri = "owner-schema-uri";
        final var primaryTitleText = "primary-title";
        final var alternativeTitleText = "alternative-title";
        final var languageId = "eng";
        final var raidAgencyUrl = "raid-agency-url";

        final var primaryTitle = new Title()
                .text(primaryTitleText)
                .type(new TitleType()
                        .id(SchemaValues.PRIMARY_TITLE_TYPE.getUri()))
                .language(new Language().id(languageId));

        final var alternativeTitle = new Title()
                .text(alternativeTitleText)
                .type(new TitleType()
                        .id(SchemaValues.ALTERNATIVE_TITLE_TYPE.getUri()));

        final var owner = new Owner()
                .id(ownerId)
                .schemaUri(ownerSchemaUri);

        final var registrationAgency = new RegistrationAgency()
                .id(registrationAgencyId)
                .schemaUri(registrationAgencySchemaUri);

        final var organisation = new Organisation()
                .id(organisationId)
                .schemaUri(organisationSchemaUri)
                .role(List.of(new OrganisationRole().id(SchemaValues.LEAD_RESEARCH_ORGANISATION_ROLE.getUri())));

        final var funder = new Organisation()
                .id(funderId)
                .schemaUri(organisationSchemaUri)
                .role(List.of(new OrganisationRole().id(SchemaValues.FUNDER_ORGANISATION_ROLE.getUri())));

        final var date = new Date().startDate("2020");

        final var description = new Description();
        final var contributor = new Contributor().status("AUTHENTICATED");
        final var relatedObject = new RelatedObject()
                .type(new RelatedObjectType().id(SchemaValues.BOOK_OBJECT_TYPE.getUri()))
                .category(List.of(new RelatedObjectCategory().id(SchemaValues.INPUT_RELATED_OBJECT_CATEGORY.getUri())));
        final var fundingRelatedObject = new RelatedObject()
                .type(new RelatedObjectType().id(SchemaValues.FUNDING_OBJECT_TYPE.getUri()))
                .category(List.of(new RelatedObjectCategory().id(SchemaValues.INPUT_RELATED_OBJECT_CATEGORY.getUri())));
        final var alternateIdentifier = new AlternateIdentifier();
        final var relatedRaid = new RelatedRaid();
        final var access = new Access()
                .type(new AccessType().id(SchemaValues.ACCESS_TYPE_EMBARGOED.getUri()));

        final var request = new RaidCreateRequest()
                .identifier(new Id()
                        .registrationAgency(registrationAgency)
                        .owner(owner)
                        .raidAgencyUrl(raidAgencyUrl)
                )
                .organisation(List.of(organisation, funder))
                .title(List.of(alternativeTitle, primaryTitle))
                .date(date)
                .description(List.of(description))
                .contributor(List.of(contributor))
                .relatedObject(List.of(fundingRelatedObject, relatedObject))
                .alternateIdentifier(List.of(alternateIdentifier))
                .relatedRaid(List.of(relatedRaid))
                .access(access);

        final var registrationAgencyContributor = new DataciteContributor()
                .setContributorType("RegistrationAgency");
        final var organisationContributor = new DataciteContributor().setContributorType("Other");
        final var publisher = new DatacitePublisher();
        final var dataciteDate = new DataciteDate();
        final var primaryDataciteTitle = new DataciteTitle()
                .setTitle(primaryTitleText)
                .setLang(languageId);
        final var alternativeDataciteTitle = new DataciteTitle()
                .setTitle(alternativeTitleText)
                .setTitleType("Other");
        final var dataciteDescription = new DataciteDescription();
        final var creator = new DataciteCreator();
        final var fundingReference = new DataciteFundingReference();
        final var relatedIdentifier = new DataciteRelatedIdentifier();
        final var dataciteAlternateIdentifier = new DataciteAlternateIdentifier();
        final var dataciteRaidAgencyUrl = new DataciteAlternateIdentifier().setAlternateIdentifier(raidAgencyUrl).setAlternateIdentifierType("RaidAgencyUrl");
        final var relatedRaidIdentifier = new DataciteRelatedIdentifier();
        final var types = new DataciteTypes();
        final var landingPrefix = "landing-prefix/";

        when(titleFactory.create(primaryTitle)).thenReturn(primaryDataciteTitle);
        when(titleFactory.create(alternativeTitle)).thenReturn(alternativeDataciteTitle);

        when(contributorFactory.create(registrationAgency)).thenReturn(registrationAgencyContributor);
        when(contributorFactory.create(organisation)).thenReturn(organisationContributor);
        when(dateFactory.create(date)).thenReturn(dataciteDate);
        when(publisherFactory.create(owner)).thenReturn(publisher);
        when(descriptionFactory.create(description)).thenReturn(dataciteDescription);
        when(creatorFactory.create(contributor)).thenReturn(creator);
        when(fundingReferenceFactory.create(funder)).thenReturn(fundingReference);
        when(relatedIdentifierFactory.create(relatedObject)).thenReturn(relatedIdentifier);
        when(alternateIdentifierFactory.create(alternateIdentifier)).thenReturn(dataciteAlternateIdentifier);
        when(alternateIdentifierFactory.create(request.getIdentifier())).thenReturn(dataciteRaidAgencyUrl);

        when(relatedIdentifierFactory.create(relatedRaid)).thenReturn(relatedRaidIdentifier);
        when(typesFactory.create()).thenReturn(types);
        when(identifierProperties.getLandingPrefix()).thenReturn(landingPrefix);

        final var result = attributesDtoFactory.create(request, handle);

        assertThat(result.getContributors(), is(List.of(registrationAgencyContributor, organisationContributor)));
        assertThat(result.getPublisher(), is(publisher));
        assertThat(result.getDates(), is(List.of(dataciteDate)));
        assertThat(result.getTitles(), is(List.of(primaryDataciteTitle, alternativeDataciteTitle)));
        assertThat(result.getDescriptions(), is(List.of(dataciteDescription)));
        assertThat(result.getCreators(), is(List.of(creator)));
        assertThat(result.getFundingReferences(), is(List.of(fundingReference)));
        assertThat(result.getRelatedIdentifiers(), is(List.of(relatedIdentifier, relatedRaidIdentifier)));
        assertThat(result.getAlternateIdentifiers(), is(List.of(dataciteRaidAgencyUrl, dataciteAlternateIdentifier)));
        assertThat(result.getTypes(), is(types));
        assertThat(result.getUrl(), is(landingPrefix + handle));
        assertThat(result.getEvent(), is("register"));
    }
}