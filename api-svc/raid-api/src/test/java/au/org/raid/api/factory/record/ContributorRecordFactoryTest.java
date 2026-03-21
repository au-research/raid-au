package au.org.raid.api.factory.record;

import au.org.raid.idl.raidv2.model.Contributor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ContributorRecordFactoryTest {
    private final ContributorRecordFactory contributorRecordFactory = new ContributorRecordFactory();

    @Test
    @DisplayName("All fields are set")
    void fieldsAreSet() {
        final var pid = "contributor-pid";
        final var schemaUri = "contributor-schema_uri";
        final var schemaId = 123;

        final var contributor = new Contributor()
                .id(pid)
                .schemaUri(schemaUri);

        final var result = contributorRecordFactory.create(contributor, schemaId);

        assertThat(result.getPid(), is(pid));
        assertThat(result.getSchemaId(), is(schemaId));
    }
}