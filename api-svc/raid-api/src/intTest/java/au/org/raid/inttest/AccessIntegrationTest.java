package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.AccessStatement;
import au.org.raid.idl.raidv2.model.AccessType;
import au.org.raid.idl.raidv2.model.AccessTypeIdEnum;
import au.org.raid.idl.raidv2.model.AccessTypeSchemaUriEnum;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import au.org.raid.inttest.service.RaidApiValidationException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static au.org.raid.fixtures.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    @DisplayName("Mint with invalid language schemaUri fails")
    void invalidLanguageSchemeUri() {
        createRequest.getAccess().getStatement().getLanguage().setSchemaUri(null);

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
    @DisplayName("Mint with empty language schemaUri fails")
    void emptyLanguageSchemeUri() {
        createRequest.getAccess().getStatement().getLanguage().setSchemaUri(null);

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
                            .errorType("notSet")
                            .message("field must be set")
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
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Mint raid with valid open access type")
    void mintOpenAccess() {
        createRequest.getAccess()
                .type(new AccessType()
                        .id(AccessTypeIdEnum.fromValue(OPEN_ACCESS_TYPE))
                        .schemaUri(AccessTypeSchemaUriEnum.fromValue(ACCESS_TYPE_SCHEMA_URI))
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
                        .id(AccessTypeIdEnum.fromValue(EMBARGOED_ACCESS_TYPE))
                        .schemaUri(AccessTypeSchemaUriEnum.fromValue(ACCESS_TYPE_SCHEMA_URI))
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
                        .id(AccessTypeIdEnum.fromValue(EMBARGOED_ACCESS_TYPE))
                        .schemaUri(AccessTypeSchemaUriEnum.fromValue(ACCESS_TYPE_SCHEMA_URI))
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

    @Test
    @DisplayName("Mint with closed access type fails")
    void blankAccessStatement() {
        final var accessType = new AccessType();
        accessType.setId(null);
        accessType.setSchemaUri(null);
        createRequest.getAccess()
                .type(accessType)
                .statement(new AccessStatement().text("Closed"));

        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(2);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("access.type.schemaUri")
                            .errorType("notSet")
                            .message("field must be set"),
            new ValidationFailure()
                            .fieldId("access.type.id")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            failOnError(e);
        }
    }

    @Test
    @DisplayName("Mint with open access type fails with missing schemaUri")
    void missingSchemeUri() {
        createRequest.getAccess()
                .type(new AccessType()
                        .id(AccessTypeIdEnum.fromValue(OPEN_ACCESS_TYPE))
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

    @Test
    @DisplayName("Mint with open access type fails with blank schemaUri")
    void blankSchemeUri() {
        final var blankSchemaUriType = new AccessType()
                .id(AccessTypeIdEnum.fromValue(OPEN_ACCESS_TYPE));
        blankSchemaUriType.setSchemaUri(null);
        createRequest.getAccess()
                .type(blankSchemaUriType);
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

    @Test
    @DisplayName("Mint with open access type fails with missing type")
    void missingType() {
        createRequest.getAccess()
                .type(new AccessType()
                        .schemaUri(AccessTypeSchemaUriEnum.fromValue(ACCESS_TYPE_SCHEMA_URI))
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

    @Test
    @DisplayName("Mint with open access type fails with blank type")
    void blankType() {
        final var blankIdType = new AccessType()
                .schemaUri(AccessTypeSchemaUriEnum.fromValue(ACCESS_TYPE_SCHEMA_URI));
        blankIdType.setId(null);
        createRequest.getAccess()
                .type(blankIdType);
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
}