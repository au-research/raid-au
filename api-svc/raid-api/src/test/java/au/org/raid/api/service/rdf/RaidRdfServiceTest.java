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
        id.setSchemaUri("https://raid.org");
        
        // Set registration agency
        var registrationAgency = new RegistrationAgency();
        registrationAgency.setId("https://ror.org/02stey378");
        registrationAgency.setSchemaUri("https://ror.org");
        id.setRegistrationAgency(registrationAgency);
        
        // Set owner
        var owner = new Owner();
        owner.setId("https://ror.org/02stey378");
        owner.setSchemaUri("https://ror.org");
        owner.setServicePoint(20000003L);
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
        titleType.setId("https://vocabulary.raid.org/title.type.schema/5");
        titleType.setSchemaUri("https://vocabulary.raid.org/title.type.schema/376");
        title.setType(titleType);
        
        var language = new Language();
        language.setId("eng");
        language.setSchemaUri("https://iso639-3.sil.org/");
        title.setLanguage(language);
        
        titles.add(title);
        raidDto.setTitle(titles);
        
        // Set description
        var descriptions = new ArrayList<Description>();
        var description = new Description();
        description.setText("This is a sample RAID for testing");
        
        var descriptionType = new DescriptionType();
        descriptionType.setId("https://vocabulary.raid.org/description.type.schema/318");
        descriptionType.setSchemaUri("https://vocabulary.raid.org/description.type.schema/320");
        description.setType(descriptionType);
        description.setLanguage(language);
        
        descriptions.add(description);
        raidDto.setDescription(descriptions);
        
        // Add access information
        var access = new Access();
        var accessType = new AccessType();
        accessType.setId("https://vocabularies.coar-repositories.org/access_rights/c_abf2/");
        accessType.setSchemaUri("https://vocabularies.coar-repositories.org/access_rights/");
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
        contributor.setSchemaUri("https://orcid.org/");
        contributor.setEmail("researcher@example.org");
        contributor.setUuid("12345678-abcd-1234-efgh-1234567890ab");
        contributor.setLeader(true);
        contributor.setContact(true);
        
        var positions = new ArrayList<ContributorPosition>();
        var position = new ContributorPosition();
        position.setId("https://vocabulary.raid.org/contributor.position.schema/307");
        position.setSchemaUri("https://vocabulary.raid.org/contributor.position.schema/305");
        position.setStartDate("2023-01-01");
        positions.add(position);
        contributor.setPosition(positions);
        
        var roles = new ArrayList<ContributorRole>();
        var role = new ContributorRole();
        role.setId("https://credit.niso.org/contributor-roles/conceptualization/");
        role.setSchemaUri("https://credit.niso.org/");
        roles.add(role);
        contributor.setRole(roles);
        
        contributors.add(contributor);
        raidDto.setContributor(contributors);
        
        // Add an organization
        var organizations = new ArrayList<Organisation>();
        var organisation = new Organisation();
        organisation.setId("https://ror.org/038sjwq14");
        organisation.setSchemaUri("https://ror.org/");
        
        var orgRoles = new ArrayList<OrganisationRole>();
        var orgRole = new OrganisationRole();
        orgRole.setId("https://vocabulary.raid.org/organisation.role.schema/182");
        orgRole.setSchemaUri("https://vocabulary.raid.org/organisation.role.schema/359");
        orgRole.setStartDate("2023-01-01");
        orgRoles.add(orgRole);
        organisation.setRole(orgRoles);
        
        organizations.add(organisation);
        raidDto.setOrganisation(organizations);
        
        // Add subjects
        var subjects = new ArrayList<Subject>();
        var subject = new Subject();
        subject.setId("https://linked.data.gov.au/def/anzsrc-for/2020/3702");
        subject.setSchemaUri("https://vocabs.ardc.edu.au/viewById/316");
        
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
        spatialCoverage.setSchemaUri("https://www.geonames.org/");
        
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
        relatedObject.setSchemaUri("https://doi.org/");
        
        var relatedObjectType = new RelatedObjectType();
        relatedObjectType.setId("https://vocabulary.raid.org/relatedObject.type.schema/258");
        relatedObjectType.setSchemaUri("https://vocabulary.raid.org/relatedObject.type.schema/329");
        relatedObject.setType(relatedObjectType);
        
        var categories = new ArrayList<RelatedObjectCategory>();
        var category = new RelatedObjectCategory();
        category.setId("https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/input.json");
        category.setSchemaUri("https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/category/v1");
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