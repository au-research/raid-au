package au.org.raid.api.service.rdf;

import au.org.raid.idl.raidv2.model.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RaidRdfServiceTest {
    private final RaidRdfService raidRdfService = new RaidRdfService();

    @Test
    @DisplayName("Should convert RaidDto to RDF model with basic properties")
    void shouldConvertRaidDtoToRdfModel() {
        // Given
        var raidDto = createSampleRaidDto();
        
        // When
        Model model = raidRdfService.toRdfModel(raidDto);
        
        // Then
        assertThat(model).isNotNull();
        assertThat(model.size()).isGreaterThan(0);
        
        // Verify the raid resource exists
        var raidResource = model.getResource("https://raid.org/123.456/abc");
        assertThat(raidResource).isNotNull();
        
        // Verify it has the correct type
        assertThat(raidResource.hasProperty(RDF.type)).isTrue();
        
        // Verify title is present
        Property hasTitle = model.getProperty("https://raid.org/schema#", "hasTitle");
        assertThat(raidResource.hasProperty(hasTitle)).isTrue();
        
        // Verify description is present
        Property hasDescription = model.getProperty("https://raid.org/schema#", "hasDescription");
        assertThat(raidResource.hasProperty(hasDescription)).isTrue();
        
        // Verify contributor is present
        Property hasContributor = model.getProperty("https://raid.org/schema#", "hasContributor");
        assertThat(raidResource.hasProperty(hasContributor)).isTrue();
        
        // Verify organisation is present
        Property hasOrganisation = model.getProperty("https://raid.org/schema#", "hasOrganisation");
        assertThat(raidResource.hasProperty(hasOrganisation)).isTrue();
        
        // Verify subject is present
        Property hasSubject = model.getProperty(DCTerms.NS, "subject");
        assertThat(raidResource.hasProperty(hasSubject)).isTrue();
        
        // Verify access is present
        Property hasAccess = model.getProperty("https://raid.org/schema#", "hasAccess");
        assertThat(raidResource.hasProperty(hasAccess)).isTrue();
    }
    
    @Test
    @DisplayName("Should include all complex nested structures in RDF model")
    void shouldIncludeComplexNestedStructures() {
        // Given
        var raidDto = createSampleRaidDto();
        
        // When
        Model model = raidRdfService.toRdfModel(raidDto);
        
        // Then - Verify nested contributor properties
        Property hasContributor = model.getProperty("https://raid.org/schema#", "hasContributor");
        Resource contributorResource = raidResource(model).getProperty(hasContributor).getObject().asResource();
        
        Property hasRole = model.getProperty("https://raid.org/schema#", "hasRole");
        assertThat(contributorResource.hasProperty(hasRole)).isTrue();
        
        Property hasPosition = model.getProperty("https://raid.org/schema#", "hasPosition");
        assertThat(contributorResource.hasProperty(hasPosition)).isTrue();
        
        // Verify nested organization properties
        Property hasOrganisation = model.getProperty("https://raid.org/schema#", "hasOrganisation");
        Resource orgResource = raidResource(model).getProperty(hasOrganisation).getObject().asResource();
        assertThat(orgResource.hasProperty(hasRole)).isTrue();
    }
    
    private Resource raidResource(Model model) {
        return model.getResource("https://raid.org/123.456/abc");
    }
    
    private RaidDto createSampleRaidDto() {
        var raidDto = new RaidDto();
        
        // Set identifier
        var id = new Id();
        id.setId("https://raid.org/123.456/abc");
        id.setSchemaUri(RaidIdentifierSchemaURIEnum.HTTPS_RAID_ORG_);
        
        // Set registration agency
        var registrationAgency = new RegistrationAgency();
        registrationAgency.setId("https://ror.org/02stey378");
        registrationAgency.setSchemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_);
        id.setRegistrationAgency(registrationAgency);
        
        // Set owner
        var owner = new Owner();
        owner.setId("https://ror.org/02stey378");
        owner.setSchemaUri(RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_);
        owner.setServicePoint(new BigDecimal(20000003L));
        id.setOwner(owner);
        
        // Set license
        id.setLicense("Creative Commons CC-0");
        id.setVersion(1);
        id.setRaidAgencyUrl("https://raid.agency.example/123.456/abc");
        
        raidDto.setIdentifier(id);
        
        // Set date
        var date = new au.org.raid.idl.raidv2.model.Date();
        date.setStartDate("2023-01-01");
        date.setEndDate("2023-12-31");
        raidDto.setDate(date);
        
        // Set title
        var titles = new ArrayList<Title>();
        var title = new Title();
        title.setText("Sample RAID");
        title.setStartDate("2023-01-01");
        
        var titleType = new TitleType();
        titleType.setId(TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_5);
        titleType.setSchemaUri(TitleTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_376);
        title.setType(titleType);
        
        var language = new Language();
        language.setId("eng");
        language.setSchemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML);
        title.setLanguage(language);
        
        titles.add(title);
        raidDto.setTitle(titles);
        
        // Set description
        var descriptions = new ArrayList<Description>();
        var description = new Description();
        description.setText("This is a sample RAID for testing");
        
        var descriptionType = new DescriptionType();
        descriptionType.setId(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_318);
        descriptionType.setSchemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320);
        description.setType(descriptionType);
        description.setLanguage(language);
        
        descriptions.add(description);
        raidDto.setDescription(descriptions);
        
        // Add access information
        var access = new Access();
        var accessType = new AccessType();
        accessType.setId(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_ABF2_);
        accessType.setSchemaUri(AccessTypeSchemaUriEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_);
        access.setType(accessType);
        
        var accessStatement = new AccessStatement();
        accessStatement.setText("This is an open access RAID");
        accessStatement.setLanguage(language);
        access.setStatement(accessStatement);
        
        raidDto.setAccess(access);
        
        // Add a contributor
        var contributors = new ArrayList<Contributor>();
        var contributor = new Contributor();
        contributor.setId("https://orcid.org/0000-0002-1825-0097");
        contributor.setSchemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_);
        contributor.setEmail("researcher@example.org");
        contributor.setUuid("12345678-abcd-1234-efgh-1234567890ab");
        contributor.setLeader(true);
        contributor.setContact(true);
        
        var positions = new ArrayList<ContributorPosition>();
        var position = new ContributorPosition();
        position.setId(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307);
        position.setSchemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305);
        position.setStartDate("2023-01-01");
        positions.add(position);
        contributor.setPosition(positions);
        
        var roles = new ArrayList<ContributorRole>();
        var role = new ContributorRole();
        role.setId(ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_CONCEPTUALIZATION_);
        role.setSchemaUri(ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_);
        roles.add(role);
        contributor.setRole(roles);
        
        contributors.add(contributor);
        raidDto.setContributor(contributors);
        
        // Add an organization
        var organizations = new ArrayList<Organisation>();
        var organisation = new Organisation();
        organisation.setId("https://ror.org/038sjwq14");
        organisation.setSchemaUri(OrganizationSchemaUriEnum.HTTPS_ROR_ORG_);
        
        var orgRoles = new ArrayList<OrganisationRole>();
        var orgRole = new OrganisationRole();
        orgRole.setId(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182);
        orgRole.setSchemaUri(OrganizationRoleSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_359);
        orgRole.setStartDate("2023-01-01");
        orgRoles.add(orgRole);
        organisation.setRole(orgRoles);
        
        organizations.add(organisation);
        raidDto.setOrganisation(organizations);
        
        // Add subjects
        var subjects = new ArrayList<Subject>();
        var subject = new Subject();
        subject.setId("https://linked.data.gov.au/def/anzsrc-for/2020/3702");
        subject.setSchemaUri(SubjectSchemaURIEnum.HTTPS_VOCABS_ARDC_EDU_AU_VIEW_BY_ID_316);
        
        var keywords = new ArrayList<SubjectKeyword>();
        var keyword = new SubjectKeyword();
        keyword.setText("Computer Science");
        keyword.setLanguage(language);
        keywords.add(keyword);
        subject.setKeyword(keywords);
        
        subjects.add(subject);
        raidDto.setSubject(subjects);
        
        // Add spatial coverage
        var spatialCoverages = new ArrayList<SpatialCoverage>();
        var spatialCoverage = new SpatialCoverage();
        spatialCoverage.setId("https://www.geonames.org/2158177/melbourne.html");
        spatialCoverage.setSchemaUri(SpatialCoverageSchemaUriEnum.HTTPS_WWW_GEONAMES_ORG_);
        
        var places = new ArrayList<SpatialCoveragePlace>();
        var place = new SpatialCoveragePlace();
        place.setText("Melbourne");
        place.setLanguage(language);
        places.add(place);
        spatialCoverage.setPlace(places);
        
        spatialCoverages.add(spatialCoverage);
        raidDto.setSpatialCoverage(spatialCoverages);
        
        // Add related objects
        var relatedObjects = new ArrayList<RelatedObject>();
        var relatedObject = new RelatedObject();
        relatedObject.setId("https://doi.org/10.12345/67890");
        relatedObject.setSchemaUri(RelatedObjectSchemaUriEnum.HTTP_DOI_ORG_);
        
        var relatedObjectType = new RelatedObjectType();
        relatedObjectType.setId(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_258);
        relatedObjectType.setSchemaUri(RelatedObjectTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_329);
        relatedObject.setType(relatedObjectType);
        
        var categories = new ArrayList<RelatedObjectCategory>();
        var category = new RelatedObjectCategory();
        category.setId(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190);
        category.setSchemaUri(RelatedObjectCategorySchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_SCHEMA_385);
        categories.add(category);
        relatedObject.setCategory(categories);
        
        relatedObjects.add(relatedObject);
        raidDto.setRelatedObject(relatedObjects);
        
        // Add alternate URLs
        var alternateUrls = new ArrayList<AlternateUrl>();
        var alternateUrl = new AlternateUrl();
        alternateUrl.setUrl("https://example.org/raid/123.456/abc");
        alternateUrls.add(alternateUrl);
        raidDto.setAlternateUrl(alternateUrls);
        
        // Add alternate identifiers
        var alternateIdentifiers = new ArrayList<AlternateIdentifier>();
        var alternateIdentifier = new AlternateIdentifier();
        alternateIdentifier.setId("hdl:10.123/456");
        alternateIdentifier.setType("Handle");
        alternateIdentifiers.add(alternateIdentifier);
        raidDto.setAlternateIdentifier(alternateIdentifiers);
        
        return raidDto;
    }
}