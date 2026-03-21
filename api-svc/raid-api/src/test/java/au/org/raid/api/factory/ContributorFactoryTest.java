package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ContributorRole;
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
        final var id = "_id";
        final var schemaUri = "schema-uri";
        final var leader = true;
        final var contact = true;
        final var positions = List.of(new ContributorPosition());
        final var roles = List.of(new ContributorRole());

        final var result = contributorFactory.create(id, schemaUri, leader, contact, positions, roles);

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
        assertThat(result.getLeader(), is(leader));
        assertThat(result.getContact(), is(contact));
        assertThat(result.getPosition(), is(positions));
        assertThat(result.getRole(), is(roles));
    }
}