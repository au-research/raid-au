package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.ContributorSchemaUriEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ContributorFactoryTest {
    private final ContributorFactory contributorFactory = new ContributorFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = "https://orcid.org/12345";
        final var schemaUri = ContributorSchemaUriEnum.HTTPS_ORCID_ORG_;
        final var leader = true;
        final var contact = true;
        final var positions = List.of(new ContributorPosition());
        final var roles = List.of(new ContributorRole());

        final var result = contributorFactory.create(id, schemaUri.getValue(), leader, contact, positions, roles);

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
        assertThat(result.getLeader(), is(leader));
        assertThat(result.getContact(), is(contact));
        assertThat(result.getPosition(), is(positions));
        assertThat(result.getRole(), is(roles));
    }
}