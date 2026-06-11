package au.org.raid.api.factory.datacite;

import au.org.raid.api.config.properties.IdentifierProperties;
import au.org.raid.api.model.datacite.doi.*;
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
        final var organisationId = "organisation-id";
        final var funderId = "funder-id";
        final var ownerId = "owner-id";
        final var primaryTitleText = "primary-title";
        final var alternativeTitleText = "alternative-title";
        final var languageId = "eng";
        final var raidAgencyUrl = "raid-agency-url";

        final var primaryTitle = new Title()
                .text(primaryTitleText)
                .type(new TitleType()
                        .id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_5))
                .language(new Language().id(languageId));

        final var alternativeTitle = new Title()
                .text(alternativeTitleText)
                .type(new TitleType()
                        .id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_4));

        final var owner = new Owner()
                .id(ownerId)
                .schemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_);

        final var registrationAgency = new RegistrationAgency()
                .id(registrationAgencyId)
                .schemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_);

        final var organisation = new Organisation()
                .id(organisationId)
                .schemaUri(OrganizationSchemaUriEnum.HTTPS_ROR_ORG_)
                .role(List.of(new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182)));

        final var funder = new Organisation()
                .id(funderId)
                .schemaUri(OrganizationSchemaUriEnum.HTTPS_ROR_ORG_)
                .role(List.of(new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_186)));

        final var date = new Date().startDate("2020");

        final var description = new Description();
        final var contributor = new Contributor()
                .status("AWAITING_AUTHENTICATION");

        final var relatedObject = new RelatedObject()
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_258))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));
        final var fundingRelatedObject = new RelatedObject()
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_272))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));
        final var alternateIdentifier = new AlternateIdentifier();
        final var relatedRaid = new RelatedRaid();
        final var access = new Access()
                .type(new AccessType().id(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_ABF2_));

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
        final var organisationId = "organisation-id";
        final var funderId = "funder-id";
        final var ownerId = "owner-id";
        final var primaryTitleText = "primary-title";
        final var alternativeTitleText = "alternative-title";
        final var languageId = "eng";
        final var raidAgencyUrl = "raid-agency-url";

        final var primaryTitle = new Title()
                .text(primaryTitleText)
                .type(new TitleType()
                        .id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_5))
                .language(new Language().id(languageId));

        final var alternativeTitle = new Title()
                .text(alternativeTitleText)
                .type(new TitleType()
                        .id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_4));

        final var owner = new Owner()
                .id(ownerId)
                .schemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_);

        final var registrationAgency = new RegistrationAgency()
                .id(registrationAgencyId)
                .schemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_);

        final var organisation = new Organisation()
                .id(organisationId)
                .schemaUri(OrganizationSchemaUriEnum.HTTPS_ROR_ORG_)
                .role(List.of(new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182)));

        final var funder = new Organisation()
                .id(funderId)
                .schemaUri(OrganizationSchemaUriEnum.HTTPS_ROR_ORG_)
                .role(List.of(new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_186)));

        final var date = new Date().startDate("2020");

        final var description = new Description();
        final var contributor = new Contributor()
                .status("AUTHENTICATED");

        final var relatedObject = new RelatedObject()
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_258))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));
        final var fundingRelatedObject = new RelatedObject()
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_272))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));
        final var alternateIdentifier = new AlternateIdentifier();
        final var relatedRaid = new RelatedRaid();
        final var access = new Access()
                .type(new AccessType().id(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_ABF2_));

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
        final var organisationId = "organisation-id";
        final var funderId = "funder-id";
        final var ownerId = "owner-id";
        final var primaryTitleText = "primary-title";
        final var alternativeTitleText = "alternative-title";
        final var languageId = "eng";
        final var raidAgencyUrl = "raid-agency-url";

        final var primaryTitle = new Title()
                .text(primaryTitleText)
                .type(new TitleType()
                        .id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_5))
                .language(new Language().id(languageId));

        final var alternativeTitle = new Title()
                .text(alternativeTitleText)
                .type(new TitleType()
                        .id(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_4));

        final var owner = new Owner()
                .id(ownerId)
                .schemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_);

        final var registrationAgency = new RegistrationAgency()
                .id(registrationAgencyId)
                .schemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_);

        final var organisation = new Organisation()
                .id(organisationId)
                .schemaUri(OrganizationSchemaUriEnum.HTTPS_ROR_ORG_)
                .role(List.of(new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182)));

        final var funder = new Organisation()
                .id(funderId)
                .schemaUri(OrganizationSchemaUriEnum.HTTPS_ROR_ORG_)
                .role(List.of(new OrganisationRole().id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_186)));

        final var date = new Date().startDate("2020");

        final var description = new Description();
        final var contributor = new Contributor().status("AUTHENTICATED");
        final var relatedObject = new RelatedObject()
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_258))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));
        final var fundingRelatedObject = new RelatedObject()
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_272))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));
        final var alternateIdentifier = new AlternateIdentifier();
        final var relatedRaid = new RelatedRaid();
        final var access = new Access()
                .type(new AccessType().id(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_F1CF_));

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
