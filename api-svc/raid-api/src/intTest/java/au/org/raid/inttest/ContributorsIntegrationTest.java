package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.*;
import au.org.raid.inttest.factory.RaidPatchRequestFactory;
import au.org.raid.inttest.factory.RaidUpdateRequestFactory;
import au.org.raid.inttest.service.Handle;
import au.org.raid.inttest.service.RaidApiValidationException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import static au.org.raid.inttest.service.TestConstants.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

// TODO: Test that pre-existing contributors have UNVERIFIED status
// TODO: How do we know when to update pre-existing contributors - if an email is present?

public class ContributorsIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private RaidUpdateRequestFactory raidUpdateRequestFactory;

    @Autowired
    private RaidPatchRequestFactory raidPatchRequestFactory;

    @Nested
    @DisplayName("Contributor create tests")
    class ContributorCreateTests {
        @Test
        @DisplayName("Email address should not be visible after create")
        void happyPath() {
            final var createResponse = raidApi.mintRaid(createRequest);
            final var handle = new Handle(createResponse.getBody().getIdentifier().getId());
            final var readResponse = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix());
            final var raidDto = readResponse.getBody();

            assertThat(raidDto.getContributor().get(0).getEmail(), is(nullValue()));
        }

        @Test
        @DisplayName("Minting a RAiD with no contributor fails")
        void noContributors() {
            createRequest.setContributor(null);

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with no contributors");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor")
                                .errorType("notSet")
                                .message("field must be set")
                ));
            }
        }

        @Test
        @DisplayName("Minting a RAiD with empty contributor fails")
        void emptyContributors() {
            createRequest.setContributor(Collections.emptyList());

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with empty contributor");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures.size(), is(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor")
                                .errorType("notSet")
                                .message("field must be set")
                ));
            }
        }

        @Test
        @DisplayName("Minting a RAiD with missing schemaUri fails")
        void missingIdentifierSchemeUri() {
            createRequest.setContributor(List.of(
                    new Contributor()
                            .id("https://orcid.org/0000-0000-0000-0001")
                            .contact(true)
                            .leader(true)
                            .position(List.of(
                                    new ContributorPosition()
                                            .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                            .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                            .id(PRINCIPAL_INVESTIGATOR_POSITION)
                            ))
                            .role(List.of(
                                    new ContributorRole()
                                            .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                            .id(SOFTWARE_CONTRIBUTOR_ROLE)
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with missing schemaUri");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures.size(), is(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor[0].schemaUri")
                                .errorType("notSet")
                                .message("field must be set")
                ));
            }
        }

        @Test
        @DisplayName("Minting a RAiD with missing contributor id fails")
        void missingId() {
            createRequest.setContributor(List.of(
                    new Contributor()
                            .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                            .contact(true)
                            .leader(true)
                            .position(List.of(
                                    new ContributorPosition()
                                            .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                            .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                            .id(PRINCIPAL_INVESTIGATOR_POSITION)
                            ))
                            .role(List.of(
                                    new ContributorRole()
                                            .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                            .id(SOFTWARE_CONTRIBUTOR_ROLE)
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with missing contributor id");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor[0].id")
                                .errorType("notSet")
                                .message("field must be set")
                ));
            }
        }

        @Test
        @DisplayName("Minting a RAiD with empty contributor id fails")
        void emptyId() {
            createRequest.setContributor(List.of(
                    new Contributor()
                            .id(REAL_TEST_ORCID)
                            .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                            .contact(true)
                            .leader(true)
                            .id("")
                            .position(List.of(
                                    new ContributorPosition()
                                            .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                            .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                            .id(PRINCIPAL_INVESTIGATOR_POSITION)
                            ))
                            .role(List.of(
                                    new ContributorRole()
                                            .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                            .id(SOFTWARE_CONTRIBUTOR_ROLE)
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with empty contributor id");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor[0].id")
                                .errorType("notSet")
                                .message("field must be set")
                ));
            }
        }

        @Test
        @DisplayName("Minting a RAiD with null position fails")
        void nullPositions() {
            createRequest.setContributor(List.of(
                    new Contributor()
                            .id(REAL_TEST_ORCID)
                            .email(CONTRIBUTOR_EMAIL)
                            .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                            .contact(true)
                            .leader(true)
                            .role(List.of(
                                    new ContributorRole()
                                            .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                            .id(SOFTWARE_CONTRIBUTOR_ROLE)
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with null position");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor[0]")
                                .errorType("notSet")
                                .message("A contributor must have a position")
                ));
            }
        }

        @Test
        @DisplayName("Minting a RAiD with empty position fails")
        void emptyPositions() {
            createRequest.setContributor(List.of(
                    new Contributor()
                            .id(REAL_TEST_ORCID)
                            .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                            .contact(true)
                            .leader(true)
                            .email(CONTRIBUTOR_EMAIL)
                            .position(Collections.emptyList())
                            .role(List.of(
                                    new ContributorRole()
                                            .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                            .id(SOFTWARE_CONTRIBUTOR_ROLE)
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with empty position");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor[0]")
                                .errorType("notSet")
                                .message("A contributor must have a position")
                ));
            }
        }

        @Test
        @DisplayName("Minting a RAiD with missing contact fails")
        void missingContact() {
            createRequest.setContributor(List.of(
                    new Contributor()
                            .id(REAL_TEST_ORCID)
                            .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                            .email(CONTRIBUTOR_EMAIL)
                            .leader(true)
                            .position(List.of(
                                    new ContributorPosition()
                                            .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                            .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                            .id(OTHER_PARTICIPANT_POSITION)
                            )).role(List.of(
                                    new ContributorRole()
                                            .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                            .id(SOFTWARE_CONTRIBUTOR_ROLE)
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with missing leader position");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor")
                                .errorType("notSet")
                                .message("At least one contributor must be flagged as a project contact")
                ));
            }
        }

        @Test
        @DisplayName("Minting a RAiD with missing leader fails")
        void missingLeader() {
            createRequest.setContributor(List.of(
                    new Contributor()
                            .id(REAL_TEST_ORCID)
                            .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                            .email(CONTRIBUTOR_EMAIL)
                            .contact(true)
                            .position(List.of(
                                    new ContributorPosition()
                                            .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                            .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                            .id(OTHER_PARTICIPANT_POSITION)
                            )).role(List.of(
                                    new ContributorRole()
                                            .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                            .id(SOFTWARE_CONTRIBUTOR_ROLE)
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with missing leader");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor")
                                .errorType("notSet")
                                .message("At least one contributor must be flagged as a project leader")
                ));
            }
        }

        @Test
        @DisplayName("Minting a RAiD with duplicate contributors fails")
        void duplicateContributors() {
            createRequest.setContributor(List.of(
                    new Contributor()
                            .id(REAL_TEST_ORCID)
                            .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                            .contact(true)
                            .leader(true)
                            .position(List.of(
                                    new ContributorPosition()
                                            .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                            .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                            .id(OTHER_PARTICIPANT_POSITION)
                            )).role(List.of(
                                    new ContributorRole()
                                            .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                            .id(SOFTWARE_CONTRIBUTOR_ROLE)
                            )),
            new Contributor()
                    .id(REAL_TEST_ORCID)
                    .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                    .contact(true)
                    .leader(true)
                    .position(List.of(
                            new ContributorPosition()
                                    .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                    .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                    .id(OTHER_PARTICIPANT_POSITION)
                    )).role(List.of(
                            new ContributorRole()
                                    .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                    .id(SOFTWARE_CONTRIBUTOR_ROLE)
                    ))
            ));

            try {
                raidApi.mintRaid(createRequest);
                fail("No exception thrown with duplicate contributors");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor[1].id")
                                .errorType("duplicateValue")
                                .message("an object with the same values appears in the list")
                ));
            }
        }

        @Test
        @DisplayName("Removing a contributor passes")
        void removeContributor() {
            createRequest.setContributor(List.of(
                    new Contributor()
                            .id(REAL_TEST_ORCID)
                            .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                            .contact(true)
                            .leader(true)
                            .position(List.of(
                                    new ContributorPosition()
                                            .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                            .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                            .id(PRINCIPAL_INVESTIGATOR_POSITION)
                            )).role(List.of(
                                    new ContributorRole()
                                            .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                            .id(SOFTWARE_CONTRIBUTOR_ROLE)
                            )),
                    new Contributor()
                            .id("https://orcid.org/0009-0002-5128-5184")
                            .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                            .position(List.of(
                                    new ContributorPosition()
                                            .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                            .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                            .id(OTHER_PARTICIPANT_POSITION)
                            )).role(List.of(
                                    new ContributorRole()
                                            .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                            .id(SOFTWARE_CONTRIBUTOR_ROLE)
                            ))
            ));

            final var response = raidApi.mintRaid(createRequest);

            final var updateRequest = response.getBody();

            updateRequest.setContributor(List.of(
                    new Contributor()
                            .id(REAL_TEST_ORCID)
                            .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                            .contact(true)
                            .leader(true)
                            .position(List.of(
                                    new ContributorPosition()
                                            .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                            .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                            .id(PRINCIPAL_INVESTIGATOR_POSITION)
                            )).role(List.of(
                                    new ContributorRole()
                                            .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                            .id(SOFTWARE_CONTRIBUTOR_ROLE)
                            ))
            ));

            try {
                raidApi.mintRaid(createRequest);
            } catch (Exception e) {
                fail(e);
            }
        }

        @Nested
        @DisplayName("Position tests...")
        class ContributorPositionTests {
            @Test
            @DisplayName("Minting a RAiD with missing position schemaUri fails")
            void missingPositionSchemeUri() {
                createRequest.setContributor(List.of(
                        new Contributor()
                                .id(REAL_TEST_ORCID)
                                .contact(true)
                                .leader(true)
                                .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                                .email("https://orcid.org/0000-0000-0000-0001")
                                .position(List.of(
                                        new ContributorPosition()
                                                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                .id(PRINCIPAL_INVESTIGATOR_POSITION)
                                )).role(List.of(
                                        new ContributorRole()
                                                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                                .id(SOFTWARE_CONTRIBUTOR_ROLE)
                                ))
                ));

                try {
                    raidApi.mintRaid(createRequest);
                    fail("No exception thrown with missing contributor schemaUri");
                } catch (RaidApiValidationException e) {
                    final var failures = e.getFailures();
                    assertThat(failures, hasSize(1));
                    assertThat(failures, contains(
                            new ValidationFailure()
                                    .fieldId("contributor[0].position[0].schemaUri")
                                    .errorType("notSet")
                                    .message("field must be set")
                    ));
                }
            }

            @Test
            @DisplayName("Minting a RAiD with missing position type fails")
            void missingPositionType() {
                createRequest.setContributor(List.of(
                        new Contributor()
                                .id(REAL_TEST_ORCID)
                                .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                                .contact(true)
                                .leader(true)
                                .email(CONTRIBUTOR_EMAIL)
                                .position(List.of(
                                        new ContributorPosition()
                                                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                ))
                                .role(List.of(
                                        new ContributorRole()
                                                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                                .id(SOFTWARE_CONTRIBUTOR_ROLE)
                                ))
                ));

                try {
                    raidApi.mintRaid(createRequest);
                    fail("No exception thrown with missing contributor schemaUri");
                } catch (RaidApiValidationException e) {
                    final var failures = e.getFailures();
                    assertThat(failures, hasSize(1));
                    assertThat(failures, contains(
                            new ValidationFailure()
                                    .fieldId("contributor[0].position[0].id")
                                    .errorType("notSet")
                                    .message("field must be set")
                    ));
                }
            }

            @Test
            @DisplayName("Minting a RAiD with a contributor with overlapping positions fails")
            void overlappingPositions() {
                createRequest.setContributor(List.of(
                        new Contributor()
                                .id(REAL_TEST_ORCID)
                                .contact(true)
                                .leader(true)
                                .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                                .email(CONTRIBUTOR_EMAIL)
                                .position(List.of(
                                        new ContributorPosition()
                                                .id(PRINCIPAL_INVESTIGATOR_POSITION)
                                                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                                .startDate(LocalDate.now().minusYears(2).format(DateTimeFormatter.ISO_LOCAL_DATE)),
                                        new ContributorPosition()
                                                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_308)
                                                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                                ))
                                .role(List.of(
                                        new ContributorRole()
                                                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                                .id(SOFTWARE_CONTRIBUTOR_ROLE)
                                ))
                ));

                try {
                    raidApi.mintRaid(createRequest);
                    fail("No exception thrown with missing contributor schemaUri");
                } catch (RaidApiValidationException e) {
                    final var failures = e.getFailures();
                    assertThat(failures, hasSize(1));
                    assertThat(failures, contains(
                            new ValidationFailure()
                                    .fieldId("contributor[0].position[1].startDate")
                                    .errorType("invalidValue")
                                    .message("Contributors can only hold one position at any given time. This position conflicts with contributor[0].position[0]")
                    ));
                }
            }
        }

        @Nested
        @DisplayName("Role tests...")
        class ContributorRoleTests {
            @Test
            @DisplayName("Minting a RAiD with missing role schemaUri fails")
            void missingRoleSchemeUri() {
                createRequest.setContributor(List.of(
                        new Contributor()
                                .id(REAL_TEST_ORCID)
                                .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                                .contact(true)
                                .leader(true)
                                .email(CONTRIBUTOR_EMAIL)
                                .position(List.of(
                                        new ContributorPosition()
                                                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                .id(PRINCIPAL_INVESTIGATOR_POSITION)
                                ))
                                .role(List.of(
                                        new ContributorRole()
//              .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                                .id(SOFTWARE_CONTRIBUTOR_ROLE)
                                ))
                ));

                try {
                    raidApi.mintRaid(createRequest);
                    fail("No exception thrown with missing role schemaUri");
                } catch (RaidApiValidationException e) {
                    final var failures = e.getFailures();
                    assertThat(failures, hasSize(1));
                    assertThat(failures, contains(
                            new ValidationFailure()
                                    .fieldId("contributor[0].role[0].schemaUri")
                                    .errorType("notSet")
                                    .message("field must be set")
                    ));
                }
            }

            @Test
            @DisplayName("Minting a RAiD with missing role type fails")
            void missingPositionType() {
                createRequest.setContributor(List.of(
                        new Contributor()
                                .id(REAL_TEST_ORCID)
                                .schemaUri(CONTRIBUTOR_IDENTIFIER_SCHEMA_URI)
                                .email(CONTRIBUTOR_EMAIL)
                                .contact(true)
                                .leader(true)
                                .position(List.of(
                                        new ContributorPosition()
                                                .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                                .id(OTHER_PARTICIPANT_POSITION)
                                ))
                                .role(List.of(
                                        new ContributorRole()
                                                .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                ))
                ));

                try {
                    raidApi.mintRaid(createRequest);
                    fail("No exception thrown with missing role type");
                } catch (RaidApiValidationException e) {
                    final var failures = e.getFailures();
                    assertThat(failures, hasSize(1));
                    assertThat(failures, contains(
                            new ValidationFailure()
                                    .fieldId("contributor[0].role[0].id")
                                    .errorType("notSet")
                                    .message("field must be set")
                    ));
                }
            }
        }
    }

    @Nested
    @DisplayName("Contributor update tests")
    class ContributorUpdateTests {

        @Test
        @DisplayName("Should not be able to update id of contributor with PUT request")
        void updateId() {
            final var createResponse = raidApi.mintRaid(createRequest);
            final var raidDto = createResponse.getBody();
            final var handle = new Handle(raidDto.getIdentifier().getId());

            final var uuid = raidDto.getContributor().get(0).getUuid();

            raidDto.getContributor().get(0).setId(REAL_TEST_ORCID);

            try {
                raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(raidDto));
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor[0]")
                                .errorType("notFound")
                                .message("Contributor not found with PID (%s) and UUID (%s)".formatted(REAL_TEST_ORCID, uuid))
                ));
            }
        }

        @Test
        @Disabled("Email not currently supported")
        @DisplayName("Should not be able to update status of contributor with PUT request")
        void updateStatus() {
            createRequest.getContributor().get(0).email("awaiting-authentication@test.raid.org.au");
            createRequest.getContributor().get(0).id(null);

            final var createResponse = raidApi.mintRaid(createRequest);
            final var raidDto = createResponse.getBody();
            final var handle = new Handle(raidDto.getIdentifier().getId());

            raidDto.getContributor().get(0).setStatus("AUTHENTICATED");

            final var updateResponse = raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(raidDto));
            assertThat(updateResponse.getBody().getContributor().get(0).getStatus(), is("AWAITING_AUTHENTICATION"));
        }

        @Test
        @DisplayName("Updating a RAiD with no contributors fails")
        void noContributors() {
            final var createResponse = raidApi.mintRaid(createRequest);
            final var raidDto = createResponse.getBody();
            final var handle = new Handle(raidDto.getIdentifier().getId());

            raidDto.contributor(null);

            try {
                raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(raidDto));
                fail("No exception thrown with no contributors");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor")
                                .errorType("notSet")
                                .message("field must be set")
                ));
            }
        }

        @Test
        @DisplayName("Updating a RAiD with missing contact fails")
        void missingContact() {
            final var createResponse = raidApi.mintRaid(createRequest);

            final var raidDto = createResponse.getBody();

            raidDto.getContributor().get(0).contact(false);

            final var handle = new Handle(raidDto.getIdentifier().getId());

            try {
                raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(raidDto));
                fail("No exception thrown with missing leader position");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor")
                                .errorType("notSet")
                                .message("At least one contributor must be flagged as a project contact")
                ));
            }
        }

        @Test
        @DisplayName("Updating a RAiD with missing leader fails")
        void missingLeader() {
            final var createResponse = raidApi.mintRaid(createRequest);

            final var raidDto = createResponse.getBody();

            raidDto.getContributor().get(0).leader(false);

            final var handle = new Handle(raidDto.getIdentifier().getId());

            try {
                raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(raidDto));
                fail("No exception thrown with missing leader position");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor")
                                .errorType("notSet")
                                .message("At least one contributor must be flagged as a project leader")
                ));
            }
        }

        @Test
        @DisplayName("Updating a RAiD with empty position fails")
        void emptyPositions() {
            final var createResponse = raidApi.mintRaid(createRequest);

            final var raidDto = createResponse.getBody();

            raidDto.getContributor().get(0).position(List.of());

            final var handle = new Handle(raidDto.getIdentifier().getId());

            try {
                raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(raidDto));
                fail("No exception thrown with empty position");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor[0]")
                                .errorType("notSet")
                                .message("A contributor must have a position")
                ));
            }
        }

        @Test
        @DisplayName("Updating a RAiD with null positions fails")
        void nullPositions() {
            final var createResponse = raidApi.mintRaid(createRequest);

            final var raidDto = createResponse.getBody();

            raidDto.getContributor().get(0).position(null);

            final var handle = new Handle(raidDto.getIdentifier().getId());

            try {
                raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(raidDto));
                fail("No exception thrown with empty position");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor[0].position")
                                .errorType("notSet")
                                .message("field must be set")
                ));
            }
        }

        @Nested
        @DisplayName("Position tests...")
        class ContributorPositionTests {
            @Test
            @DisplayName("Updating a RAiD with missing position schemaUri fails")
            void missingPositionSchemeUri() {
                final var createResponse = raidApi.mintRaid(createRequest);
                final var raidDto = createResponse.getBody();
                final var handle = new Handle(raidDto.getIdentifier().getId());

                raidDto.getContributor().get(0).getPosition().get(0).schemaUri(null);

                try {
                    raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(raidDto));
                    fail("No exception thrown with missing contributor schemaUri");
                } catch (RaidApiValidationException e) {
                    final var failures = e.getFailures();
                    assertThat(failures, hasSize(1));
                    assertThat(failures, contains(
                            new ValidationFailure()
                                    .fieldId("contributor[0].position[0].schemaUri")
                                    .errorType("notSet")
                                    .message("field must be set")
                    ));
                }
            }

            @Test
            @DisplayName("Updating a RAiD with missing position id fails")
            void missingPositionId() {
                final var createResponse = raidApi.mintRaid(createRequest);
                final var raidDto = createResponse.getBody();
                final var handle = new Handle(raidDto.getIdentifier().getId());

                raidDto.getContributor().get(0).getPosition().get(0).id(null);

                try {
                    raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(raidDto));
                    fail("No exception thrown with missing contributor schemaUri");
                } catch (RaidApiValidationException e) {
                    final var failures = e.getFailures();
                    assertThat(failures, hasSize(1));
                    assertThat(failures, contains(
                            new ValidationFailure()
                                    .fieldId("contributor[0].position[0].id")
                                    .errorType("notSet")
                                    .message("field must be set")
                    ));
                }
            }

            @Test
            @DisplayName("Updating a RAiD with a contributor with overlapping positions fails")
            void overlappingPositions() {
                final var createResponse = raidApi.mintRaid(createRequest);
                final var raidDto = createResponse.getBody();
                final var handle = new Handle(raidDto.getIdentifier().getId());

                raidDto.getContributor().get(0).position(List.of(
                        new ContributorPosition()
                                .id(PRINCIPAL_INVESTIGATOR_POSITION)
                                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                .startDate(LocalDate.now().minusYears(2).format(DateTimeFormatter.ISO_LOCAL_DATE)),
                        new ContributorPosition()
                                .id(ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_308)
                                .schemaUri(ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305)
                                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                ));

                try {
                    raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(raidDto));
                    fail("No exception thrown with missing contributor schemaUri");
                } catch (RaidApiValidationException e) {
                    final var failures = e.getFailures();
                    assertThat(failures, hasSize(1));
                    assertThat(failures, contains(
                            new ValidationFailure()
                                    .fieldId("contributor[0].position[1].startDate")
                                    .errorType("invalidValue")
                                    .message("Contributors can only hold one position at any given time. This position conflicts with contributor[0].position[0]")
                    ));
                }
            }
        }

        @Nested
        @DisplayName("Role tests...")
        class ContributorRoleTests {
            @Test
            @DisplayName("Updating a RAiD with missing role schemaUri fails")
            void missingRoleSchemeUri() {
                final var createResponse = raidApi.mintRaid(createRequest);
                final var raidDto = createResponse.getBody();
                final var handle = new Handle(raidDto.getIdentifier().getId());

                raidDto.getContributor().get(0).getRole().get(0).schemaUri(null);

                try {
                    raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(raidDto));
                    fail("No exception thrown with missing role schemaUri");
                } catch (RaidApiValidationException e) {
                    final var failures = e.getFailures();
                    assertThat(failures, hasSize(1));
                    assertThat(failures, contains(
                            new ValidationFailure()
                                    .fieldId("contributor[0].role[0].schemaUri")
                                    .errorType("notSet")
                                    .message("field must be set")
                    ));
                }
            }

            @Test
            @DisplayName("Updating a RAiD with missing role id fails")
            void missingPositionId() {
                final var createResponse = raidApi.mintRaid(createRequest);
                final var raidDto = createResponse.getBody();
                final var handle = new Handle(raidDto.getIdentifier().getId());

                raidDto.getContributor().get(0).getRole().get(0).id(null);

                try {
                    raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), raidUpdateRequestFactory.create(raidDto));
                    fail("No exception thrown with missing role type");
                } catch (RaidApiValidationException e) {
                    final var failures = e.getFailures();
                    assertThat(failures, hasSize(1));
                    assertThat(failures, contains(
                            new ValidationFailure()
                                    .fieldId("contributor[0].role[0].id")
                                    .errorType("notSet")
                                    .message("field must be set")
                    ));
                }
            }
        }
    }

    @Nested
    @DisplayName("Contributor patch tests")
    class ContributorPatchTests {

        @Test
        @DisplayName("Should add authenticated contributor to raid")
        void authenticatedContributor() {
            final var createResponse = raidApi.mintRaid(createRequest);
            final var handle = new Handle(createResponse.getBody().getIdentifier().getId());

            final var readResponse = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix());
            final var raidDto = readResponse.getBody();

            assertThat(raidDto.getContributor().get(0).getStatus(), is("AUTHENTICATED"));
            assertThat(raidDto.getContributor().get(0).getId(), is(REAL_TEST_ORCID));
        }

        @Test
        @Disabled("Email not currently supported")
        @DisplayName("Should set awaiting-authentication contributor")
        void awaitingAuthenticationContributor() {
            createRequest.getContributor().get(0).setEmail("awaiting-authentication@test.raid.org.au");
            createRequest.getContributor().get(0).setId(null);

            final var createResponse = raidApi.mintRaid(createRequest);
            final var handle = new Handle(createResponse.getBody().getIdentifier().getId());

            final var readResponse = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix());
            final var raidDto = readResponse.getBody();

            assertThat(raidDto.getContributor().get(0).getStatus(), is("AWAITING_AUTHENTICATION"));
            assertThat(raidDto.getContributor().get(0).getId(), is(nullValue()));
            assertThat(raidDto.getContributor().get(0).getUuid(), is("4b932e7c-f7c2-4bd6-93d0-0244f47bdbcb"));
        }

        @Test
        @Disabled("Email not currently supported")
        @DisplayName("Should set authentication-failed contributor")
        void authenticationFailedContributor() {
            createRequest.getContributor().get(0).setEmail("authentication-failed@test.raid.org.au");
            createRequest.getContributor().get(0).setId(null);

            final var createResponse = raidApi.mintRaid(createRequest);
            final var handle = new Handle(createResponse.getBody().getIdentifier().getId());

            final var readResponse = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix());
            final var raidDto = readResponse.getBody();

            assertThat(raidDto.getContributor().get(0).getStatus(), is("AUTHENTICATION_FAILED"));
            assertThat(raidDto.getContributor().get(0).getId(), is(nullValue()));
            assertThat(raidDto.getContributor().get(0).getUuid(), is("de8cb78e-3cb6-424d-9537-3b6a0b15604c"));
        }

        @Test
        @DisplayName("Should be able to successfully patch a contributor")
        void happyPath() {
            final var createResponse = raidApi.mintRaid(createRequest);
            final var raidDto = createResponse.getBody();
            final var handle = new Handle(raidDto.getIdentifier().getId());

            raidDto.getContributor().get(0).setId(REAL_TEST_ORCID);
            raidDto.getContributor().get(0).setStatus("AUTHENTICATED");

            final var patchResponse = raidApi.patchRaid(handle.getPrefix(), handle.getSuffix(), raidPatchRequestFactory.create(raidDto));
            final var updateRequest = raidUpdateRequestFactory.create(patchResponse.getBody());
            raidApi.updateRaid(handle.getPrefix(), handle.getSuffix(), updateRequest);

        }

        @Test
        @DisplayName("Should fail with invalid status")
        void invalidStatus() {
            final var createResponse = raidApi.mintRaid(createRequest);
            final var raidDto = createResponse.getBody();
            final var handle = new Handle(raidDto.getIdentifier().getId());

            raidDto.getContributor().get(0).setId(REAL_TEST_ORCID);
            raidDto.getContributor().get(0).setStatus("invalid");

            try {
                raidApi.patchRaid(handle.getPrefix(), handle.getSuffix(), raidPatchRequestFactory.create(raidDto));
                fail("Patch should fail with invalid status");
            } catch (RaidApiValidationException e) {
                final var failures = e.getFailures();
                assertThat(failures, hasSize(1));
                assertThat(failures, contains(
                        new ValidationFailure()
                                .fieldId("contributor[0].status")
                                .errorType("invalidValue")
                                .message("Contributor status should be one of AUTHENTICATED, UNAUTHENTICATED, AWAITING_AUTHENTICATION, AUTHENTICATION_FAILED, AUTHENTICATION_REVOKED")
                ));
            }
        }
    }
}