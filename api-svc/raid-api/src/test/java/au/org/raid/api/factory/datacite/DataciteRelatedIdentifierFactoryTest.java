package au.org.raid.api.factory.datacite;

import au.org.raid.api.util.SchemaValues;
import au.org.raid.api.vocabularies.datacite.RelatedIdentifierType;
import au.org.raid.api.vocabularies.datacite.RelationType;
import au.org.raid.api.vocabularies.datacite.ResourceTypeGeneral;
import au.org.raid.api.vocabularies.raid.RelatedRaidType;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DataciteRelatedIdentifierFactoryTest {

    private DataciteRelatedIdentifierFactory dataciteRelatedIdentifierFactory = new DataciteRelatedIdentifierFactory();

    @Test
    @DisplayName("Create related identifier with 'Book' resource type")
    public void bookResourceType() {
        final var id = "_id";
        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_258))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Book"));
        assertThat(result.getRelationType(), is("References"));
    }

    @Test
    @DisplayName("Create related identifier with 'Output Management Plan' resource type")
    public void outputManagementPlanResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_ARKS_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_247))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("ARK"));
        assertThat(result.getResourceTypeGeneral(), is("OutputManagementPlan"));
        assertThat(result.getRelationType(), is("References"));
    }

    @Test
    @DisplayName("Create related identifier with 'Conference Poster' resource type")
    public void conferencePosterResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_WWW_ISBN_INTERNATIONAL_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_248))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("ISBN"));
        assertThat(result.getResourceTypeGeneral(), is("Text"));
        assertThat(result.getRelationType(), is("References"));
    }

    @Test
    @DisplayName("Create related identifier with 'Workfloe' resource type")
    public void workflowResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_SCICRUNCH_ORG_RESOLVER_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_249))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("URL"));
        assertThat(result.getResourceTypeGeneral(), is("Workflow"));
        assertThat(result.getRelationType(), is("References"));
    }

    @Test
    @DisplayName("Create related identifier with 'Journal Article' resource type")
    public void journalArticleResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_ARCHIVE_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_250))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("URL"));
        assertThat(result.getResourceTypeGeneral(), is("JournalArticle"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Standard' resource type")
    public void standardResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_251))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_190)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Standard"));
        assertThat(result.getRelationType(), is("IsReferencedBy"));
    }
    @Test
    @DisplayName("Create related identifier with 'Report' resource type")
    public void reportResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_252))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_192)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Report"));
        assertThat(result.getRelationType(), is("IsSupplementedBy"));
    }
    @Test
    @DisplayName("Create related identifier with 'Dissertation' resource type")
    public void dissertationResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_253))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Dissertation"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Preprint' resource type")
    public void preprintResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_254))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Preprint"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Data Paper' resource type")
    public void dataPaperResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_255))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("DataPaper"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Computational Notebook' resource type")
    public void computationalNotebookResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_256))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("ComputationalNotebook"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Image' resource type")
    public void imageResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_257))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Image"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Software' resource type")
    public void softwareResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_259))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Software"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Event' resource type")
    public void eventResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_260))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Event"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Sound' resource type")
    public void soundResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_261))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Sound"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Conference Proceeding' resource type")
    public void conferenceProceedingResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_262))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("ConferenceProceeding"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Model' resource type")
    public void modelResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_263))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Model"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Conference Paper' resource type")
    public void conferencePaperResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_264))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("ConferencePaper"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Text' resource type")
    public void textResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_265))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Text"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Instrument' resource type")
    public void instrumentResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_266))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Instrument"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Learning Object' resource type")
    public void learningObjectResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_267))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Other"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Prize' resource type")
    public void prizeResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_268))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Other"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Dataset' resource type")
    public void datasetResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_269))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Dataset"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Physical Object' resource type")
    public void physicalObjectResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_270))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("PhysicalObject"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Book Chapter' resource type")
    public void bookChapterResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_271))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("BookChapter"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Funding' resource type")
    public void fundingResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_272))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Other"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Audiovisual' resource type")
    public void audiovisualResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_273))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Audiovisual"));
        assertThat(result.getRelationType(), is("References"));
    }
    @Test
    @DisplayName("Create related identifier with 'Service' resource type")
    public void serviceResourceType() {
        final var id = "_id";

        final var relatedObject = new RelatedObject()
                .id(id)
                .schemaUri(RelatedObjectSchemaUriEnum.HTTPS_DOI_ORG_)
                .type(new RelatedObjectType().id(RelatedObjectTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_TYPE_SCHEMA_274))
                .category(List.of(new RelatedObjectCategory().id(RelatedObjectCategoryIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_OBJECT_CATEGORY_ID_191)));

        final var result = dataciteRelatedIdentifierFactory.create(relatedObject);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is("DOI"));
        assertThat(result.getResourceTypeGeneral(), is("Service"));
        assertThat(result.getRelationType(), is("References"));
    }

    @Test
    @DisplayName("Create from alternate url")
    public void alternateUrl(){
        final var url = "_url";

        final var alternateUrl = new AlternateUrl()
                .url(url);

        final var result = dataciteRelatedIdentifierFactory.create(alternateUrl);

        assertThat(result.getRelatedIdentifier(), is(url));
        assertThat(result.getRelationType(), is("IsDocumentedBy"));
        assertThat(result.getRelatedIdentifierType(), is("URL"));
        assertThat(result.getResourceTypeGeneral(), is("Other"));
    }

    @Test
    @DisplayName("Create from related raid with 'Continues' type")
    void relatedRaidContinues() {
        final var id = "_id";
        final var typeId = RelatedRaidType.CONTINUES.getUri();

        final var relatedRaid = new RelatedRaid()
                .id(id)
                .type(new au.org.raid.idl.raidv2.model.RelatedRaidType()
                        .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_204));
        final var result = dataciteRelatedIdentifierFactory.create(relatedRaid);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is(RelatedIdentifierType.DOI.getName()));
        assertThat(result.getRelationType(), is(RelationType.CONTINUES.getName()));
        assertThat(result.getResourceTypeGeneral(), is(ResourceTypeGeneral.PROJECT.getName()));
    }

    @Test
    @DisplayName("Create from related raid with 'IsContinuedBy' type")
    void relatedRaidIsContinuedBy() {
        final var id = "_id";
        final var typeId = RelatedRaidType.IS_CONTINUED_BY.getUri();

        final var relatedRaid = new RelatedRaid()
                .id(id)
                .type(new au.org.raid.idl.raidv2.model.RelatedRaidType()
                        .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_203));
        final var result = dataciteRelatedIdentifierFactory.create(relatedRaid);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is(RelatedIdentifierType.DOI.getName()));
        assertThat(result.getRelationType(), is(RelationType.IS_CONTINUED_BY.getName()));
        assertThat(result.getResourceTypeGeneral(), is(ResourceTypeGeneral.PROJECT.getName()));
    }
    @Test
    @DisplayName("Create from related raid with 'IsPartOf' type")
    void relatedRaidIsPartOf() {
        final var id = "_id";
        final var typeId = RelatedRaidType.IS_PART_OF.getUri();

        final var relatedRaid = new RelatedRaid()
                .id(id)
                .type(new au.org.raid.idl.raidv2.model.RelatedRaidType()
                        .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_202));
        final var result = dataciteRelatedIdentifierFactory.create(relatedRaid);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is(RelatedIdentifierType.DOI.getName()));
        assertThat(result.getRelationType(), is(RelationType.IS_PART_OF.getName()));
        assertThat(result.getResourceTypeGeneral(), is(ResourceTypeGeneral.PROJECT.getName()));
    }

    @Test
    @DisplayName("Create from related raid with 'HasPart' type")
    void relatedRaidHasPart() {
        final var id = "_id";
        final var typeId = RelatedRaidType.HAS_PART.getUri();

        final var relatedRaid = new RelatedRaid()
                .id(id)
                .type(new au.org.raid.idl.raidv2.model.RelatedRaidType()
                        .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_201));
        final var result = dataciteRelatedIdentifierFactory.create(relatedRaid);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is(RelatedIdentifierType.DOI.getName()));
        assertThat(result.getRelationType(), is(RelationType.HAS_PART.getName()));
        assertThat(result.getResourceTypeGeneral(), is(ResourceTypeGeneral.PROJECT.getName()));
    }

    @Test
    @DisplayName("Create from related raid with 'IsDerivedFrom' type")
    void relatedRaidIsDerivedFrom() {
        final var id = "_id";
        final var typeId = RelatedRaidType.IS_DERIVED_FROM.getUri();

        final var relatedRaid = new RelatedRaid()
                .id(id)
                .type(new au.org.raid.idl.raidv2.model.RelatedRaidType()
                        .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_200));
        final var result = dataciteRelatedIdentifierFactory.create(relatedRaid);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is(RelatedIdentifierType.DOI.getName()));
        assertThat(result.getRelationType(), is(RelationType.IS_DERIVED_FROM.getName()));
        assertThat(result.getResourceTypeGeneral(), is(ResourceTypeGeneral.PROJECT.getName()));
    }

    @Test
    @DisplayName("Create from related raid with 'IsSourceOf' type")
    void relatedRaidIsSourceOf() {
        final var id = "_id";
        final var typeId = RelatedRaidType.IS_SOURCE_OF.getUri();

        final var relatedRaid = new RelatedRaid()
                .id(id)
                .type(new au.org.raid.idl.raidv2.model.RelatedRaidType()
                        .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_199));
        final var result = dataciteRelatedIdentifierFactory.create(relatedRaid);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is(RelatedIdentifierType.DOI.getName()));
        assertThat(result.getRelationType(), is(RelationType.IS_SOURCE_OF.getName()));
        assertThat(result.getResourceTypeGeneral(), is(ResourceTypeGeneral.PROJECT.getName()));
    }

    @Test
    @DisplayName("Create from related raid with 'Obsoletes' type")
    void relatedRaidObsoletes() {
        final var id = "_id";
        final var typeId = RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_198;

        final var relatedRaid = new RelatedRaid()
                .id(id)
                .type(new au.org.raid.idl.raidv2.model.RelatedRaidType()
                        .id(typeId));
        final var result = dataciteRelatedIdentifierFactory.create(relatedRaid);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is(RelatedIdentifierType.DOI.getName()));
        assertThat(result.getRelationType(), is(RelationType.OBSOLETES.getName()));
        assertThat(result.getResourceTypeGeneral(), is(ResourceTypeGeneral.PROJECT.getName()));
    }

    @Test
    @DisplayName("Create from related raid with 'IsObsoletedBy' type")
    void relatedRaidIsObsoletedBy() {
        final var id = "_id";
        final var typeId = RelatedRaidType.IS_OBSOLETED_BY.getUri();

        final var relatedRaid = new RelatedRaid()
                .id(id)
                .type(new au.org.raid.idl.raidv2.model.RelatedRaidType()
                        .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_205));
        final var result = dataciteRelatedIdentifierFactory.create(relatedRaid);

        assertThat(result.getRelatedIdentifier(), is(id));
        assertThat(result.getRelatedIdentifierType(), is(RelatedIdentifierType.DOI.getName()));
        assertThat(result.getRelationType(), is(RelationType.IS_OBSOLETED_BY.getName()));
        assertThat(result.getResourceTypeGeneral(), is(ResourceTypeGeneral.PROJECT.getName()));
    }
}
