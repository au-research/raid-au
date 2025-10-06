package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.AccessStatement;
import au.org.raid.idl.raidv2.model.AccessType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import au.org.raid.inttest.service.RaidApiValidationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static au.org.raid.inttest.service.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
public class AccessIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Mint with invalid language id fails")
    void invalidLanguageId() {
        createRequest.getAccess().getStatement().getLanguage().setId("xxx");

        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("access.statement.language.id")
                            .errorType("invalidValue")
                            .message("id does not exist within the given schema")
            );
        } catch (Exception e) {
            failOnError(e);
        }
    }
/*
    @Test
    @DisplayName("Mint with invalid language schemaUri fails")
    void invalidLanguageSchemeUri() {
        createRequest.getAccess().getStatement().getLanguage().schemaUri("http://localhost");

        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("access.statement.language.schemaUri")
                            .errorType("invalidValue")
                            .message("schema is unknown/unsupported")
            );
        } catch (Exception e) {
            failOnError(e);
        }
    }
*/
    @Test
    @DisplayName("Mint with empty language schemaUri fails")
    void nullLanguageSchemeUri() {
        createRequest.getAccess().getStatement().getLanguage().schemaUri(null);

        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("access.statement.language.schemaUri")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Mint with empty language id fails")
    void emptyLanguageId() {
        createRequest.getAccess().getStatement().getLanguage().setId("");

        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("access.statement.language.id")
                            .errorType("invalidValue")
                            .message("has invalid/unsupported value - must match \"^\\s*\\S.*$\"")
            );
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Mint with null language id fails")
    void nullLanguageId() {
        createRequest.getAccess().getStatement().getLanguage().setId(null);

        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("access.statement.language.id")
                            .errorType("notSet")
                            .message("field must be set")
            );
        }
    }

    @Test
    @DisplayName("Mint raid with valid open access type")
    void mintOpenAccess() {
        createRequest.getAccess()
                .type(new AccessType()
                        .id(OPEN_ACCESS_TYPE)
                        .schemaUri(ACCESS_TYPE_SCHEMA_URI)
                );

        try {
            raidApi.mintRaid(createRequest);
        } catch (Exception e) {
            failOnError(e);
        }
    }


    @Test
    @DisplayName("Mint with valid embargoed access type")
    void mintEmbargoedAccess() {
        createRequest.getAccess()
                .type(new AccessType()
                        .id(EMBARGOED_ACCESS_TYPE)
                        .schemaUri(ACCESS_TYPE_SCHEMA_URI)
                )
                .embargoExpiry(LocalDate.now())
                .statement(new AccessStatement().text("Embargoed"));
        try {
            raidApi.mintRaid(createRequest);
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Mint with embargoed access type fails with missing embargoExpiry")
    void missingEmbargoExpiry() {
        createRequest.getAccess()
                .type(new AccessType()
                        .id(EMBARGOED_ACCESS_TYPE)
                        .schemaUri(ACCESS_TYPE_SCHEMA_URI)
                )
                .statement(new AccessStatement().text("Embargoed"));
        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("access.embargoExpiry")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            failOnError(e);
        }
    }
/*
    @Test
    @DisplayName("Mint with closed access type fails")
    void blankAccessStatement() {
        createRequest.getAccess()
                .type(new AccessType()
                        .id("https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json")
                        .schemaUri("https://github.com/au-research/raid-metadata/tree/main/scheme/access/type/v1/")
                )
                .statement(new AccessStatement().text("Closed"));

        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(2);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("access.type.schemaUri")
                            .errorType("invalidValue")
                            .message("schema is unknown/unsupported"),
            new ValidationFailure()
                            .fieldId("access.type.id")
                            .errorType("invalidValue")
                            .message("Creating closed Raids is no longer supported")
            );
        } catch (Exception e) {
            failOnError(e);
        }
    }
*/
    @Test
    @DisplayName("Mint with open access type fails with missing schemaUri")
    void missingSchemeUri() {
        createRequest.getAccess()
                .type(new AccessType()
                        .id(OPEN_ACCESS_TYPE)
                );

        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("access.type.schemaUri")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            failOnError(e);
        }
    }
/*
    @Test
    @DisplayName("Mint with open access type fails with blank schemaUri")
    void blankSchemeUri() {
        createRequest.getAccess()
                .type(new AccessType()
                        .id(OPEN_ACCESS_TYPE)
                        .schemaUri("")
                );
        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("access.type.schemaUri")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            failOnError(e);
        }
    }
*/
    @Test
    @DisplayName("Mint with open access type fails with missing type")
    void missingType() {
        createRequest.getAccess()
                .type(new AccessType()
                        .schemaUri(ACCESS_TYPE_SCHEMA_URI)
                );
        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("access.type.id")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            failOnError(e);
        }
    }
/*
    @Test
    @DisplayName("Mint with open access type fails with blank type")
    void blankType() {
        createRequest.getAccess()
                .type(new AccessType()
                        .id("")
                        .schemaUri(ACCESS_TYPE_SCHEMA_URI)
                );
        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("access.type.id")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            failOnError(e);
        }
    }

 */
}