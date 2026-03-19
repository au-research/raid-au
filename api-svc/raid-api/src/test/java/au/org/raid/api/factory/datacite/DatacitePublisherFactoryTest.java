package au.org.raid.api.factory.datacite;

import au.org.raid.api.client.ror.RorClient;
import au.org.raid.idl.raidv2.model.Owner;
import au.org.raid.idl.raidv2.model.RegistrationAgencySchemaURIEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatacitePublisherFactoryTest {
    @Mock
    private RorClient rorClient;
    @InjectMocks
    private DatacitePublisherFactory publisherFactory;

    @Test
    @DisplayName("Create sets all fields")
    void create() {
        final var id = "_id";
        final var schemaUriEnum = RegistrationAgencySchemaURIEnum.HTTPS_ROR_ORG_;
        final var name = "_name";

        final var owner = new Owner()
                .id(id)
                .schemaUri(schemaUriEnum);

        when(rorClient.getOrganisationName(id)).thenReturn(name);

        when(rorClient.getOrganisationName(id)).thenReturn(name);

        final var result = publisherFactory.create(owner);

        assertThat(result.getSchemeUri(), is(schemaUriEnum.getValue()));
        assertThat(result.getPublisherIdentifier(), is(id));
        assertThat(result.getName(), is(name));
        assertThat(result.getPublisherIdentifierScheme(), is("ROR"));
    }
}