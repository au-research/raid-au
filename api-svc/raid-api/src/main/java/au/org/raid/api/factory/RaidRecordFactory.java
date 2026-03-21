package au.org.raid.api.factory;

import au.org.raid.db.jooq.enums.Metaschema;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.idl.raidv2.model.RaidDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RaidRecordFactory {
    private static final String ACCESS_TYPE_OPEN =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/open.json";

    private static final String PRIMARY_TITLE_TYPE =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/primary.json";

    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final HandleFactory handleFactory;

    @SneakyThrows
    public RaidRecord create(final RaidDto raid,
                             final Integer accessTypeId,
                             final Integer accessStatementLanguageId,
                             final Integer registrationAgencyOrganisationId,
                             final Integer ownerOrganisationId
                             ) {

        final var handle = handleFactory.create(raid.getIdentifier().getId());

        String accessStatement = null;
        if (raid.getAccess().getStatement() != null) {
            accessStatement = raid.getAccess().getStatement().getText();
        }

        return new RaidRecord()
                .setHandle(handle.toString())
                .setServicePointId(raid.getIdentifier().getOwner().getServicePoint())
                .setMetadataSchema(Metaschema.raido_metadata_schema_v2)
                .setDateCreated(LocalDateTime.now(clock))
                .setVersion(raid.getIdentifier().getVersion())
                .setStartDateString(raid.getDate().getStartDate())
                .setEndDate(raid.getDate().getEndDate())
                .setLicense(raid.getIdentifier().getLicense())
                .setAccessTypeId(accessTypeId)
                .setEmbargoExpiry(raid.getAccess().getEmbargoExpiry())
                .setAccessStatement(accessStatement)
                .setAccessStatementLanguageId(accessStatementLanguageId)
                .setSchemaUri(raid.getIdentifier().getSchemaUri())
                .setRegistrationAgencyOrganisationId(registrationAgencyOrganisationId)
                .setOwnerOrganisationId(ownerOrganisationId);
    }
}