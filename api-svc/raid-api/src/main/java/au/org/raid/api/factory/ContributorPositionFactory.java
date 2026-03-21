package au.org.raid.api.factory;

import au.org.raid.db.jooq.tables.records.RaidContributorPositionRecord;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import org.springframework.stereotype.Component;

@Component
public class ContributorPositionFactory {
    public ContributorPosition create(final RaidContributorPositionRecord raidContributorPosition, final String uri, final String schemaUri) {
        return new ContributorPosition()
                .id(uri)
                .schemaUri(schemaUri)
                .startDate(raidContributorPosition.getStartDate())
                .endDate(raidContributorPosition.getEndDate());
    }
}