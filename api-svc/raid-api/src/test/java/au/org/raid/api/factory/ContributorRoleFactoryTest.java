package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.ContributorRoleIdEnum;
import au.org.raid.idl.raidv2.model.ContributorRoleSchemaUriEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ContributorRoleFactoryTest {
    private final ContributorRoleFactory factory = new ContributorRoleFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = ContributorRoleIdEnum.HTTPS_CREDIT_NISO_ORG_CONTRIBUTOR_ROLES_CONCEPTUALIZATION_;
        final var schemaUri = ContributorRoleSchemaUriEnum.HTTPS_CREDIT_NISO_ORG_;

        final var result = factory.create(id.getValue(), schemaUri.getValue());

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
    }
}