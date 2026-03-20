package au.org.raid.api.factory;

import au.org.raid.db.jooq.tables.RaidContributorPosition;
import au.org.raid.db.jooq.tables.records.RaidContributorPositionRecord;
import au.org.raid.idl.raidv2.model.ContributorPositionIdEnum;
import au.org.raid.idl.raidv2.model.ContributorPositionSchemaUriEnum;
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
        final var uri = ContributorPositionIdEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_307;
        final var schemaUri = ContributorPositionSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_CONTRIBUTOR_POSITION_SCHEMA_305;

        final var raidContributorPosition = new RaidContributorPositionRecord()
                .setStartDate(startDate)
                .setEndDate(endDate);

        final var result = factory.create(raidContributorPosition, uri.getValue(), schemaUri.getValue());

        assertThat(result.getStartDate(), is(startDate));
        assertThat(result.getEndDate(), is(endDate));
        assertThat(result.getId(), is(uri));
        assertThat(result.getSchemaUri(), is(schemaUri));
    }
}