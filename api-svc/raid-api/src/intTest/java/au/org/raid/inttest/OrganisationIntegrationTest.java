package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.Organisation;
import au.org.raid.idl.raidv2.model.OrganisationRole;
import au.org.raid.idl.raidv2.model.OrganizationRoleIdEnum;
import au.org.raid.idl.raidv2.model.OrganizationRoleSchemaUriEnum;
import au.org.raid.idl.raidv2.model.OrganizationSchemaUriEnum;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import au.org.raid.inttest.service.RaidApiValidationException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static au.org.raid.fixtures.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class OrganisationIntegrationTest extends AbstractIntegrationTest {
    @Test
    @DisplayName("Minting a RAiD with no organisations succeeds")
    void noOrganisations() {
        createRequest.setOrganisation(null);

        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            fail("No validation failures expected");
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Test
    @DisplayName("Minting a RAiD with empty organisations succeeds")
    void emptyOrganisations() {
        createRequest.setOrganisation(Collections.emptyList());

        try {
            raidApi.mintRaid(createRequest);
        } catch (RaidApiValidationException e) {
            fail("No validation failures expected");
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Test
    @DisplayName("Minting a RAiD with missing organisation schemaUri fails")
    void missingIdentifierSchemeUri() {
        final var role = new OrganisationRole()
                .startDate("2021")
                .schemaUri(OrganizationRoleSchemaUriEnum.fromValue(ORGANISATION_ROLE_SCHEMA_URI))
                .id(OrganizationRoleIdEnum.fromValue(LEAD_RESEARCH_ORGANISATION_ROLE));
        final var organisation = new Organisation()
                .id(VALID_ROR)
                .role(List.of(role));

        createRequest.setOrganisation(List.of(organisation));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing schemaUri");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("organisation[0].schemaUri")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Test
    @DisplayName("Minting a RAiD with empty organisation schemaUri fails")
    void emptyIdentifierSchemeUri() {
        final var role = new OrganisationRole()
                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .schemaUri(OrganizationRoleSchemaUriEnum.fromValue(ORGANISATION_ROLE_SCHEMA_URI))
                .id(OrganizationRoleIdEnum.fromValue(LEAD_RESEARCH_ORGANISATION));
        final var organisation = new Organisation()
                .id(VALID_ROR)
                .role(List.of(role));
        organisation.setSchemaUri(null);

        createRequest.setOrganisation(List.of(organisation));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with empty schemaUri");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("organisation[0].schemaUri")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Test
    @DisplayName("Minting a RAiD with missing organisation id fails")
    void missingId() {
        createRequest.setOrganisation(List.of(
                new Organisation()
                        .schemaUri(OrganizationSchemaUriEnum.fromValue(ORGANISATION_IDENTIFIER_SCHEMA_URI))
                        .role(List.of(
                                new OrganisationRole()
                                        .schemaUri(OrganizationRoleSchemaUriEnum.fromValue(ORGANISATION_ROLE_SCHEMA_URI))
                                        .id(OrganizationRoleIdEnum.fromValue(LEAD_RESEARCH_ORGANISATION_ROLE))
                                        .startDate("2021")
                        ))
        ));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with missing organisation id");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("organisation[0].id")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Test
    @DisplayName("Minting a RAiD with empty organisation id fails")
    void emptyId() {
        createRequest.setOrganisation(List.of(
                new Organisation()
                        .schemaUri(OrganizationSchemaUriEnum.fromValue(ORGANISATION_IDENTIFIER_SCHEMA_URI))
                        .id("")
                        .role(List.of(
                                new OrganisationRole()
                                        .schemaUri(OrganizationRoleSchemaUriEnum.fromValue(ORGANISATION_ROLE_SCHEMA_URI))
                                        .id(OrganizationRoleIdEnum.fromValue(LEAD_RESEARCH_ORGANISATION_ROLE))
                                        .startDate("2021")))

        ));

        try {
            raidApi.mintRaid(createRequest);
            fail("No exception thrown with empty organisation id");
        } catch (RaidApiValidationException e) {
            final var failures = e.getFailures();
            assertThat(failures).hasSize(1);
            assertThat(failures).contains(
                    new ValidationFailure()
                            .fieldId("organisation[0].id")
                            .errorType("notSet")
                            .message("field must be set")
            );
        } catch (Exception e) {
            fail("Expected RaidApiValidationException");
        }
    }

    @Nested
    @DisplayName("ROR tests...")
    class RorTests {
        @Test
        @DisplayName("Minting a RAiD with invalid ror pattern fails")
        void invalidRorPattern() {
            createRequest.setOrganisation(List.of(
                    new Organisation()
                            .schemaUri(OrganizationSchemaUriEnum.fromValue(ORGANISATION_IDENTIFIER_SCHEMA_URI))
                            .id("https://ror.org/038sjwqx@")
                            .role(List.of(
                                    new OrganisationRole()
                                            .schemaUri(OrganizationRoleSchemaUriEnum.fromValue(ORGANISATION_ROLE_SCHEMA_URI))
                                            .id(OrganizationRoleIdEnum.fromValue(LEAD_RESEARCH_ORGANISATION_ROLE))
                                            .startDate("2021")
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with invalid ror pattern");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures).hasSize(1);
                assertThat(failures).contains(
                        new ValidationFailure()
                                .fieldId("organisation[0].id")
                                .errorType("invalidValue")
                                .message("has invalid/unsupported value - should match ^https://ror\\.org/[0-9a-z]{9}$")
                );
            } catch (Exception e) {
                fail("Expected RaidApiValidationException");
            }
        }

        @Test
        @DisplayName("Minting a RAiD with non-existent ror fails")
        void nonExistentRor() {
            createRequest.setOrganisation(List.of(
                    new Organisation()
                            .schemaUri(OrganizationSchemaUriEnum.fromValue(ORGANISATION_IDENTIFIER_SCHEMA_URI))
                            .id("https://ror.org/000000042")
                            .role(List.of(
                                    new OrganisationRole()
                                            .schemaUri(OrganizationRoleSchemaUriEnum.fromValue(ORGANISATION_ROLE_SCHEMA_URI))
                                            .id(OrganizationRoleIdEnum.fromValue(LEAD_RESEARCH_ORGANISATION_ROLE))
                                            .startDate("2021")
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with non-existent ror");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures).hasSize(1);
                assertThat(failures).contains(
                        new ValidationFailure()
                                .fieldId("organisation[0].id")
                                .errorType("notFound")
                                .message("This ROR does not exist")
                );
            } catch (Exception e) {
                fail("Expected RaidApiValidationException");
            }
        }
    }

    @Nested
    @DisplayName("Role tests...")
    class OrganisationRoleTests {
        @Test
        @DisplayName("Minting a RAiD with missing role schemaUri fails")
        void missingRoleSchemeUri() {
            final var role = new OrganisationRole()
                    .id(OrganizationRoleIdEnum.fromValue(LEAD_RESEARCH_ORGANISATION_ROLE))
                    .startDate("2021");
            createRequest.setOrganisation(List.of(
                    new Organisation()
                            .schemaUri(OrganizationSchemaUriEnum.fromValue(ORGANISATION_IDENTIFIER_SCHEMA_URI))
                            .id(VALID_ROR)
                            .role(List.of(role))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with missing role schemaUri");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures).hasSize(1);
                assertThat(failures).contains(
                        new ValidationFailure()
                                .fieldId("organisation[0].role[0].schemaUri")
                                .errorType("notSet")
                                .message("field must be set")
                );
            } catch (Exception e) {
                fail("Expected RaidApiValidationException");
            }
        }

        @Test
        @DisplayName("Minting a RAiD with missing role type fails")
        void missingRoleType() {
            createRequest.setOrganisation(List.of(
                    new Organisation()
                            .schemaUri(OrganizationSchemaUriEnum.fromValue(ORGANISATION_IDENTIFIER_SCHEMA_URI))
                            .id(VALID_ROR)
                            .role(List.of(
                                    new OrganisationRole()
                                            .schemaUri(OrganizationRoleSchemaUriEnum.fromValue(ORGANISATION_ROLE_SCHEMA_URI))
                                            .startDate("2021")
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with missing role type");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures).hasSize(1);
                assertThat(failures).contains(
                        new ValidationFailure()
                                .fieldId("organisation[0].role[0].id")
                                .errorType("notSet")
                                .message("field must be set")
                );
            } catch (Exception e) {
                fail("Expected RaidApiValidationException");
            }
        }

        @Test
        @DisplayName("Minting a RAiD with empty role type fails")
        void emptyRoleType() {
            final var role = new OrganisationRole()
                    .schemaUri(OrganizationRoleSchemaUriEnum.fromValue(ORGANISATION_ROLE_SCHEMA_URI))
                    .startDate("2021");
            role.setId(null);
            createRequest.setOrganisation(List.of(
                    new Organisation()
                            .schemaUri(OrganizationSchemaUriEnum.fromValue(ORGANISATION_IDENTIFIER_SCHEMA_URI))
                            .id(VALID_ROR)
                            .role(List.of(role))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with empty role type");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures).hasSize(1);
                assertThat(failures).contains(
                        new ValidationFailure()
                                .fieldId("organisation[0].role[0].id")
                                .errorType("notSet")
                                .message("field must be set")
                );
            } catch (Exception e) {
                fail("Expected RaidApiValidationException");
            }
        }

        @Disabled("TODO:RL Cannot test invalid role schemaUri with typed enum — OrganizationRoleSchemaUriEnum " +
                "has only one valid value; passing an arbitrary invalid string is not possible via the enum API")
        @Test
        @DisplayName("Minting a RAiD with invalid role schemaUri fails")
        void invalidRoleSchemeUri() {
            createRequest.setOrganisation(List.of(
                    new Organisation()
                            .schemaUri(OrganizationSchemaUriEnum.fromValue(ORGANISATION_IDENTIFIER_SCHEMA_URI))
                            .id(VALID_ROR)
                            .role(List.of(
                                    new OrganisationRole()
                                            .schemaUri(OrganizationRoleSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_359)
                                            .id(OrganizationRoleIdEnum.fromValue(LEAD_RESEARCH_ORGANISATION_ROLE))
                                            .startDate("2021")
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with invalid role schemaUri");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures).hasSize(1);
                assertThat(failures).contains(
                        new ValidationFailure()
                                .fieldId("organisation[0].role[0].schemaUri")
                                .errorType("invalidValue")
                                .message("schema is unknown/unsupported")
                );
            } catch (Exception e) {
                fail("Expected RaidApiValidationException");
            }
        }

        @Disabled("TODO:RL Cannot test invalid role id with typed enum — all OrganizationRoleIdEnum values " +
                "are valid for the schema; passing an arbitrary invalid string is not possible via the enum API")
        @Test
        @DisplayName("Minting a RAiD with invalid type for role schema fails")
        void invalidRoleTypeForScheme() {
            createRequest.setOrganisation(List.of(
                    new Organisation()
                            .schemaUri(OrganizationSchemaUriEnum.fromValue(ORGANISATION_IDENTIFIER_SCHEMA_URI))
                            .id(VALID_ROR)
                            .role(List.of(
                                    new OrganisationRole()
                                            .schemaUri(OrganizationRoleSchemaUriEnum.fromValue(ORGANISATION_ROLE_SCHEMA_URI))
                                            .id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182)
                                            .startDate("2021")
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with invalid type for role schema");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures).hasSize(1);
                assertThat(failures).contains(
                        new ValidationFailure()
                                .fieldId("organisation[0].role[0].id")
                                .errorType("invalidValue")
                                .message("id does not exist within the given schema")
                );
            } catch (Exception e) {
                fail("Expected RaidApiValidationException");
            }
        }
    }
}
