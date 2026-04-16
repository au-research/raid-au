package au.org.raid.api.factory;

import au.org.raid.db.jooq.tables.RaidContributorPosition;
import au.org.raid.db.jooq.tables.records.RaidContributorPositionRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ContributorPositionFactoryTest {
    private final ContributorPositionFactory factory = new ContributorPositionFactory();

    @Test
    @DisplayName("Sets all fields")
    void setsAllFields() {
        final var startDate = "2021";
        final var endDate = "2022";
        final var uri = "_uri";
        final var schemaUri = "schema-uri";

        final var raidContributorPosition = new RaidContributorPositionRecord()
                .setStartDate(startDate)
                .setEndDate(endDate);

        final var result = factory.create(raidContributorPosition, uri, schemaUri);

        assertThat(result.getStartDate(), is(startDate));
        assertThat(result.getEndDate(), is(endDate));
        assertThat(result.getId(), is(uri));
        assertThat(result.getSchemaUri(), is(schemaUri));
    }
}