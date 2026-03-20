package au.org.raid.api.factory;

import au.org.raid.idl.raidv2.model.AccessTypeIdEnum;
import au.org.raid.idl.raidv2.model.AccessTypeSchemaUriEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class AccessTypeFactoryTest {
    private final AccessTypeFactory factory = new AccessTypeFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var id = AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_ABF2_;
        final var schemaUri = AccessTypeSchemaUriEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_;

        final var result = factory.create(id.getValue(), schemaUri.getValue());

        assertThat(result.getId(), is(id));
        assertThat(result.getSchemaUri(), is(schemaUri));
    }
}