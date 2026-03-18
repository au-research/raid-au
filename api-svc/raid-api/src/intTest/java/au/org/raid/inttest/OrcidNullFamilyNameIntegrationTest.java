package au.org.raid.inttest;

import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.inttest.service.Handle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static au.org.raid.fixtures.TestConstants.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class OrcidNullFamilyNameIntegrationTest extends AbstractIntegrationTest {

    private static final String ORCID_WITH_NULL_FAMILY_NAME = "https://sandbox.orcid.org/0009-0009-5506-7601";

    @Nested
    @DisplayName("ORCID contributor with null family name")
    class OrcidNullFamilyName {

        @Test
        @DisplayName("should succeed when contributor ORCID profile has no family name")
        void shouldSucceedWhenContributorHasNoFamilyName() {
            final var contributor = new Contributor()
                    .schemaUri(ORCID_SCHEMA_URI)
                    .id(ORCID_WITH_NULL_FAMILY_NAME)
                    .contact(true)
                    .leader(true)
                    .position(List.of(
                            new ContributorPosition()
                                    .startDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                                    .schemaUri(CONTRIBUTOR_POSITION_SCHEMA_URI)
                                    .id(PRINCIPAL_INVESTIGATOR_POSITION)
                    ))
                    .role(List.of(
                            new ContributorRole()
                                    .schemaUri(CONTRIBUTOR_ROLE_SCHEMA_URI)
                                    .id(SOFTWARE_CONTRIBUTOR_ROLE)
                    ));

            createRequest.setContributor(List.of(contributor));

            try {
                final var createResponse = raidApi.mintRaid(createRequest);
                final var handle = new Handle(createResponse.getBody().getIdentifier().getId());

                final var readResponse = raidApi.findRaidByName(handle.getPrefix(), handle.getSuffix());
                final var raidDto = readResponse.getBody();

                final var updateRequest = raidUpdateRequestFactory.create(raidDto);

                final var updateResponse = raidApi.updateRaid(
                        handle.getPrefix(), handle.getSuffix(), updateRequest);

                assertThat(updateResponse.getBody(), is(notNullValue()));
            } catch (Exception e) {
                failOnError(e);
            }
        }
    }
}
