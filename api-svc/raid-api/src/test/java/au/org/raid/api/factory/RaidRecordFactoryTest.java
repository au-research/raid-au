package au.org.raid.api.factory;

import au.org.raid.api.service.Handle;
import au.org.raid.api.util.TestRaid;
import au.org.raid.db.jooq.enums.Metaschema;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RaidRecordFactoryTest {
    private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final HandleFactory handleFactory = mock(HandleFactory.class);
    private final RaidRecordFactory raidRecordFactory = new RaidRecordFactory(clock, objectMapper, handleFactory);

    @Test
    @DisplayName("Sets all fields from RaidDto")
    void setsAllFieldsFromRaidDto() {
        final var accessTypeId = 10;
        final var accessStatementLanguageId = 20;
        final var registrationAgencyOrganisationId = 30;
        final var ownerOrganisationId = 40;

        when(handleFactory.create(TestRaid.RAID_NAME)).thenReturn(new Handle(TestRaid.RAID_NAME));

        final var record = raidRecordFactory.create(TestRaid.RAID_DTO,
                accessTypeId,
                accessStatementLanguageId,
                registrationAgencyOrganisationId,
                ownerOrganisationId
        );

        assertThat(record.getHandle(), is(TestRaid.HANDLE));
        assertThat(record.getServicePointId(), is(TestRaid.SERVICE_POINT_ID));
        assertThat(record.getMetadataSchema(), is(Metaschema.raido_metadata_schema_v2));
        assertThat(record.getDateCreated(), is(LocalDateTime.now(clock)));
        assertThat(record.getVersion(), is(TestRaid.VERSION));
        assertThat(record.getStartDateString(), is(TestRaid.START_DATE));
        assertThat(record.getEndDate(), is(TestRaid.END_DATE));
        assertThat(record.getLicense(), is(TestRaid.LICENSE));
        assertThat(record.getAccessTypeId(), is(accessTypeId));
        assertThat(record.getEmbargoExpiry(), is(TestRaid.EMBARGO_EXPIRY));
        assertThat(record.getAccessStatement(), is(TestRaid.ACCESS_STATEMENT_TEXT));
        assertThat(record.getAccessStatementLanguageId(), is(accessStatementLanguageId));
        assertThat(record.getSchemaUri(), is(TestRaid.RAID_SCHEMA_URI));
        assertThat(record.getRegistrationAgencyOrganisationId(), is(registrationAgencyOrganisationId));
        assertThat(record.getOwnerOrganisationId(), is(ownerOrganisationId));
    }
}