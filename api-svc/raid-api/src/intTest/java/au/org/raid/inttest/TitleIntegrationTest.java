package au.org.raid.inttest;

import au.org.raid.fixtures.TestConstants;
import au.org.raid.idl.raidv2.model.Title;
import au.org.raid.idl.raidv2.model.TitleType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import au.org.raid.inttest.service.RaidApiValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class TitleIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Minting a RAiD with a title with an null language schemaUri fails")
    void nullLanguageSchemeUri() {
        createRequest.getTitle().get(0).getLanguage().schemaUri(null);

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing title");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("title[0].language.schemaUri")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a title with an empty language schemaUri fails")
    void emptyLanguageSchemeUri() {
        createRequest.getTitle().get(0).getLanguage().schemaUri("");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing title");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("title[0].language.schemaUri")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a title with an empty language id fails")
    void emptyLanguageId() {
        createRequest.getTitle().get(0).getLanguage().setId("");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing title");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("title[0].language.id")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a title with an null language fails")
    void nullLanguageId() {
        createRequest.getTitle().get(0).getLanguage().setId(null);

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing title");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("title[0].language.id")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a title with an invalid language id fails")
    void invalidLanguageId() {
        createRequest.getTitle().get(0).getLanguage().setId("xxx");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing title");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("title[0].language.id")
                    .errorType("invalidValue")
                    .message("id does not exist within the given schema")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with a title with an invalid language schema fails")
    void invalidLanguageScheme() {
        createRequest.getTitle().get(0).getLanguage().schemaUri("http://localhost");

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing title");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("title[0].language.schemaUri")
                    .errorType("invalidValue")
                    .message("schema is unknown/unsupported")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with null titles fails")
    void nullTitles() {
        createRequest.setTitle(null);

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing title");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("title")
                    .errorType("notSet")
                    .message("field must be set")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with missing title fails")
    void missingTitle() {
        createRequest.setTitle(Collections.emptyList());

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing title");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(new ValidationFailure()
                    .fieldId("title.type")
                    .errorType("missingPrimaryTitle")
                    .message("at least one primaryTitle entry must be provided")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with no primary title fails")
    void alternativeTitleOnly() {
        createRequest.getTitle().get(0).setType(new TitleType()
                .id(TestConstants.ALTERNATIVE_TITLE_TYPE)
                .schemaUri(TestConstants.TITLE_TYPE_SCHEMA_URI));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing primary title");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("title.type")
                            .errorType("missingPrimaryTitle")
                            .message("at least one primaryTitle entry must be provided")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with missing schemaUri fails")
    void missingTitleScheme() {
        createRequest.getTitle().get(0).setType(new TitleType()
                .id(TestConstants.PRIMARY_TITLE_TYPE)
        );

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing schemaUri");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("title[0].type.schemaUri")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with invalid schemaUri fails")
    void invalidTitleScheme() {
        createRequest.getTitle().get(0).setType(new TitleType()
                .id(TestConstants.PRIMARY_TITLE_TYPE)
                .schemaUri("https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v2"));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with invalid schemaUri");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("title[0].type.schemaUri")
                            .errorType("invalidValue")
                            .message("schema is unknown/unsupported")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with missing title type id fails")
    void missingTitleType() {
        final var titles = new ArrayList<>(createRequest.getTitle());

        titles.add(new Title()
                .text("Test Title")
                .type(new TitleType()
                        .schemaUri(TestConstants.TITLE_TYPE_SCHEMA_URI)
                )
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        );

        createRequest.setTitle(titles);

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing title type");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("title[1].type.id")
                            .message("field must be set")
                            .errorType("notSet")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with invalid title type id fails")
    void invalidTitleType() {
        final var titles = new ArrayList<>(createRequest.getTitle());

        titles.add(new Title()
                .type(new TitleType()
                        .id("https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/invalid.json")
                        .schemaUri(TestConstants.TITLE_TYPE_SCHEMA_URI)
                )
                .text("Test Title")
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
        );

        createRequest.setTitle(titles);

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing invalid title type id");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .message("id does not exist within the given schema")
                            .fieldId("title[1].type.id")
                            .errorType("invalidValue")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Minting a RAiD with missing startDate fails")
    void missingStartDate() {
        createRequest.getTitle().get(0).setStartDate(null);

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing startDate");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);

            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("title[0].startDate")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}