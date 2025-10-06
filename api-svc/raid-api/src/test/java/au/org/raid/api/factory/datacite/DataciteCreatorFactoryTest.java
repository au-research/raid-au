package au.org.raid.api.factory.datacite;

import au.org.raid.api.dto.ContributorLookupResponse;
import au.org.raid.api.service.OrcidIntegrationClient;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.ContributorSchemaUriEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DataciteCreatorFactoryTest {
    @Mock
    private OrcidIntegrationClient orcidIntegrationClient;

    @InjectMocks
    private DataciteCreatorFactory dataciteCreatorFactory;

    @Test
    @DisplayName("Create with contributor - ORCID")
    void createWithContributorORCID() {
        final var id = "_id";
        final var name = "_name";
        final var schemaUri = ContributorSchemaUriEnum.HTTPS_ORCID_ORG_;

        final var contributor = new Contributor()
                .id(id)
                .status("AUTHENTICATED")
                .schemaUri(schemaUri);

        final var contributorLookupResponse = ContributorLookupResponse.builder()
                .orcid(id)
                .name(name)
                .build();

        when(orcidIntegrationClient.findByOrcid(id)).thenReturn(Optional.of(contributorLookupResponse));

        final var result = dataciteCreatorFactory.create(contributor);

        assertThat(result.getName(), is(name));
        assertThat(result.getNameType(), is("Personal"));
        assertThat(result.getNameIdentifiers().get(0).getNameIdentifier(), is(id));
        assertThat(result.getNameIdentifiers().get(0).getNameIdentifierScheme(), is("ORCID"));
    }

    @Test
    @DisplayName("Create with contributor - Does not throw exception with missing name")
    void createWithContributorORCID_EmptyResponse() {
        final var id = "_id";
        final var schemaUri = "https://orcid.org/";

        final var contributor = new Contributor()
                .id(id)
                .status("AUTHENTICATED")
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ORCID_ORG_);

        final var contributorLookupResponse = ContributorLookupResponse.builder()
                .orcid(id)
                .build();

        when(orcidIntegrationClient.findByOrcid(id)).thenReturn(Optional.of(contributorLookupResponse));

        final var result = dataciteCreatorFactory.create(contributor);

        assertThat(result.getName(), is(nullValue()));
    }

    @Test
    @DisplayName("Create with contributor - ISNI")
    void createWithContributorISNI() {
        final var id = "_id";
        final var name = "_name";

        final var schemaUri = "https://isni.org/";

        final var contributor = new Contributor()
                .id(id)
                .status("AUTHENTICATED")
                .schemaUri(ContributorSchemaUriEnum.HTTPS_ISNI_ORG_);

        final var contributorLookupResponse = ContributorLookupResponse.builder()
                .orcid(id)
                .name(name)
                .build();

        when(orcidIntegrationClient.findByOrcid(id)).thenReturn(Optional.of(contributorLookupResponse));

        final var result = dataciteCreatorFactory.create(contributor);

        assertThat(result.getName(), is(name));
        assertThat(result.getNameType(), is("Personal"));
        assertThat(result.getNameIdentifiers().get(0).getNameIdentifier(), is(id));
        assertThat(result.getNameIdentifiers().get(0).getNameIdentifierScheme(), is("ISNI"));
    }
}
