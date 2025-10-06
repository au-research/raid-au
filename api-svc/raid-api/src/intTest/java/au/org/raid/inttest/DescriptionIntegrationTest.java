package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.*;
import au.org.raid.inttest.factory.RaidUpdateRequestFactory;
import au.org.raid.inttest.service.Handle;
import au.org.raid.inttest.service.RaidApiValidationException;
import au.org.raid.inttest.service.TestConstants;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import static au.org.raid.inttest.service.TestConstants.ALTERNATIVE_DESCRIPTION_TYPE;
import static au.org.raid.inttest.service.TestConstants.DESCRIPTION_TYPE_SCHEMA_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class DescriptionIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private RaidUpdateRequestFactory updateRequestFactory;

    @Test
    @DisplayName("Minting a RAiD with a description with an null language schemaUri fails")
    void nullLanguageSchemeUri() {
        createRequest.getDescription().get(0).getLanguage().schemaUri(null);

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing description");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[0].language.schemaUri")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }
/*
    @Test
    @DisplayName("Minting a RAiD with a description with an empty language schemaUri fails")
    void emptyLanguageSchemeUri() {
        createRequest.getDescription().get(0).getLanguage().schemaUri("");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing description");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[0].language.schemaUri")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a description with an empty language id fails")
    void emptyLanguageId() {
        createRequest.getDescription().get(0).getLanguage().setId("");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing description");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[0].language.id")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }
*/
    @Test
    @DisplayName("Minting a RAiD with a description with an null language id fails")
    void nullLanguageId() {
        createRequest.getDescription().get(0).getLanguage().setId(null);

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing description");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[0].language.id")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a description with an invalid language id fails")
    void invalidLanguageId() {
        createRequest.getDescription().get(0).getLanguage().setId("xxx");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing description");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[0].language.id")
                    .errorType("invalidValue")
                    .message("id does not exist within the given schema")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }
/*
    @Test
    @DisplayName("Minting a RAiD with a description with an invalid language schema fails")
    void invalidLanguageScheme() {
        createRequest.getDescription().get(0).getLanguage().schemaUri("http://localhost");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing description");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[0].language.schemaUri")
                    .errorType("invalidValue")
                    .message("schema is unknown/unsupported")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }
*/

    @Test
    @DisplayName("Minting a RAiD with missing description block succeeds")
    void missingTitle() {
        createRequest.setDescription(Collections.emptyList());

        try {
            raidApi.mintRaid(createRequest);
        } catch (Exception e) {
            fail("Description should be optional");
        }
    }

    @Test
    @DisplayName("Validation fails with missing type schemaUri")
    void missingSchemeUri() {
        createRequest.getDescription().get(0).getType().schemaUri(null);

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing schemaUri");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("description[0].type.schemaUri")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }
/*
    @Test
    @DisplayName("Validation fails with invalid type schemaUri")
    void invalidSchemeUri() {
        createRequest.getDescription().get(0).getType()
                .schemaUri("https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v2");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with invalid schemaUri");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[0].type.schemaUri")
                    .errorType("invalidValue")
                    .message("schema is unknown/unsupported")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Test
    @DisplayName("Validation fails with blank type schemaUri")
    void blankSchemeUri() {
        createRequest.getDescription().get(0).getType().schemaUri("");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with blank schemaUri");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("description[0].type.schemaUri")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }
*/
    @Test
    @DisplayName("Validation fails with missing text")
    void missingDescription() {
        createRequest.getDescription().get(0).text(null);

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing description");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[0].text")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Test
    @DisplayName("Validation fails with blank text")
    void blankDescription() {
        createRequest.getDescription().get(0).text("");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with blank description");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[0].text")
                    .errorType("invalidValue")
                    .message("has invalid/unsupported value - must match \"^\\s*\\S.*$\"")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Test
    @DisplayName("Validation fails with missing type")
    void nullType() {
        createRequest.getDescription().add(new Description()
                .text("New description"));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing type");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[1].type")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Test
    @DisplayName("Validation fails with missing type id")
    void missingId() {
        createRequest.getDescription().add(newDescription()
                .type(new DescriptionType()
                        .schemaUri(DESCRIPTION_TYPE_SCHEMA_URI))
        );

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing type id");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[1].type.id")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }
/*
    @Test
    @DisplayName("Validation fails with empty type id")
    void emptyId() {
        createRequest.getDescription().add(new Description()
                .type(new DescriptionType()
                        .id("")
                        .schemaUri(DESCRIPTION_TYPE_SCHEMA_URI))
                .text("Description text...")
        );

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing description type id");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[1].type.id")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Test
    @DisplayName("Validation fails if type is not found within schema")
    void invalidType() {
        createRequest.getDescription().add(
                new Description()
                        .type(new DescriptionType()
                                .id("https://vocabulary.raid.org/description.type.schema/60")
                                .schemaUri("https://vocabulary.raid.org/description.type.schema/320")
                        )
                        .text("description text...")
                );

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown when id not found in schema");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("description[1].type.id")
                    .errorType("invalidValue")
                    .message("id does not exist within the given schema")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }
*/
    private Description newDescription() {
        return new Description()
                .text("New description...")
                .type(new DescriptionType()
                        .id(ALTERNATIVE_DESCRIPTION_TYPE)
                        .schemaUri(DESCRIPTION_TYPE_SCHEMA_URI)
                )
                .language(new Language()
                        .schemaUri(TestConstants.LANGUAGE_SCHEMA_URI)
                        .id("eng")
                );
    }

    @Test
    @DisplayName("All valid description types")
    void happyPath() {
        final var acknowledgements = new Description()
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_392)
                        .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320)
                )
                .text("Acknowledgements")
                .language(new Language()
                        .id("eng")
                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                );

        final var alternative = new Description()
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_319)
                        .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320)
                )
                .text("Alternative")
                .language(new Language()
                        .id("eng")
                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                );


        final var primary = new Description()
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_318)
                        .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320)
                )
                .text("Primary")
                .language(new Language()
                        .id("eng")
                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                );

        final var brief = new Description()
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_3)
                        .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320)
                )
                .text("Brief")
                .language(new Language()
                        .id("eng")
                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                );

        final var methods = new Description()
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_8)
                        .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320)
                )
                .text("Methods")
                .language(new Language()
                        .id("eng")
                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                );

        final var objectives = new Description()
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_7)
                        .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320)
                )
                .text("Objectives")
                .language(new Language()
                        .id("eng")
                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                );

        final var other = new Description()
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_6)
                        .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320)
                )
                .text("Other")
                .language(new Language()
                        .id("eng")
                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                );

        final var significanceStatement = new Description()
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_9)
                        .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320)
                )
                .text("Significance Statement")
                .language(new Language()
                        .id("eng")
                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                );

        createRequest.description(List.of(
                acknowledgements,
                alternative,
                primary,
                brief,
                methods,
                objectives,
                other,
                significanceStatement
        ));

        final var createResponse = raidApi.mintRaid(createRequest);
        final var handle = new Handle(createResponse.getBody().getIdentifier().getId());
        final var readResponse = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix());
        final var raidDto = readResponse.getBody();

        assertThat(raidDto.getDescription()).contains(
                acknowledgements,
                alternative,
                primary,
                brief,
                methods,
                objectives,
                other,
                significanceStatement
        );
    }

    @Test
    @DisplayName("Description should handle multiple updates")
    void multipleUpdates() {
        final var mintResponse = raidApi.mintRaid(createRequest);

        final var mintedRaid = mintResponse.getBody();

        final var primaryDescription = mintedRaid.getDescription().get(0);

        final var handle = new Handle(mintedRaid.getIdentifier().getId());

        assertThat(mintedRaid.getDescription()).hasSize(1);

        mintedRaid.getDescription().add(new Description()
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_392)
                        .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320)
                )
                .text("Acknowledgements")
                .language(new Language()
                        .id("eng")
                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                ));

        final var updateResponse1 =
                raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequestFactory.create(mintedRaid));

        final var updatedRaid1 = updateResponse1.getBody();

        assertThat(updatedRaid1.getDescription()).hasSize(2);

        updatedRaid1.getDescription().add(new Description()
                .type(new DescriptionType()
                        .id(DescriptionTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_7)
                        .schemaUri(DescriptionTypeSchemaURIEnum.HTTPS_VOCABULARY_RAID_ORG_DESCRIPTION_TYPE_SCHEMA_320)
                )
                .text("Objectives")
                .language(new Language()
                        .id("eng")
                        .schemaUri(LanguageSchemaURIEnum.HTTPS_WWW_ISO_ORG_STANDARD_74575_HTML)
                ));

        final var updateResponse2 =
                raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequestFactory.create(updatedRaid1));

        final var updatedRaid2 = updateResponse2.getBody();

        assertThat(updatedRaid2.getDescription()).hasSize(3);

        updatedRaid2.description(List.of(primaryDescription));

        final var updateResponse3 =
                raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequestFactory.create(updatedRaid2));

        final var updatedRaid3 = updateResponse3.getBody();
        assertThat(updatedRaid2.getDescription()).hasSize(1);
    }
}