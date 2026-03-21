package au.org.raid.api.factory.datacite;

import au.org.raid.api.client.ror.RorClient;
import au.org.raid.api.client.ror.dto.Name;
import au.org.raid.api.client.ror.dto.RorSchemaV21;
import au.org.raid.idl.raidv2.model.Organisation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataciteFundingReferenceFactoryTest {
    @Mock
    private RorClient rorClient;

    @InjectMocks
    private DataciteFundingReferenceFactory fundingReferenceFactory;

    @Test
    @DisplayName("Create sets all fields")
    void setAllFields() {
        final var id = "_id";
        final var schemaUri = "schema-uri";
        final var organisationName = "organisation-name";

        when(rorClient.getOrganisationName(id)).thenReturn(organisationName);

        final var organisation = new Organisation()
                .id(id)
                .schemaUri(schemaUri);

        final var result = fundingReferenceFactory.create(organisation);

        assertThat(result.getFunderName(), is(organisationName));
        assertThat(result.getFunderIdentifier(), is(id));
        assertThat(result.getFunderIdentifierType(), is("ROR"));
        assertThat(result.getSchemeUri(), is(schemaUri));
    }
}