package au.org.raid.api.factory;

import au.org.raid.api.config.properties.IdentifierProperties;
import au.org.raid.db.jooq.tables.records.ServicePointRecord;
import au.org.raid.idl.raidv2.model.Id;
import au.org.raid.idl.raidv2.model.Owner;
import au.org.raid.idl.raidv2.model.RegistrationAgency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdFactoryTest {
    private static final String SCHEMA_URI = "__schema-uri__";
    private static final String IDENTIFIER_REGISTRATION_AGENCY = "identifier-registration-agency";
    private static final String IDENTIFIER_OWNER = "identifier-owner";
    private static final Long IDENTIFIER_SERVICE_POINT = 999L;
    private static final String LICENSE = "raid-license";
    private static final String LANDING_PREFIX = "__landing-prefix__";
    private static final String NAME_PREFIX = "__name-prefix__";

    @Mock
    private IdentifierProperties identifierProperties;

    @InjectMocks
    private IdFactory idFactory;

    @Test
    @DisplayName("Sets all fields when creating from IdentifierUrl and ServicePointRecord")
    void createFromIdentifierUrlAndServicePointRecord() {
        final var prefix = "_prefix_";
        final var suffix = "_suffix_:";
        final var handle = "%s/%s".formatted(prefix, suffix);
        final var servicePointRecord = new ServicePointRecord()
                .setIdentifierOwner(IDENTIFIER_OWNER)
                .setId(IDENTIFIER_SERVICE_POINT);
        final var rorSchemaUri = "https://ror.org/";

        when(identifierProperties.getRegistrationAgencyIdentifier()).thenReturn(IDENTIFIER_REGISTRATION_AGENCY);
        when(identifierProperties.getLicense()).thenReturn(LICENSE);
        when(identifierProperties.getLandingPrefix()).thenReturn(LANDING_PREFIX);
        when(identifierProperties.getNamePrefix()).thenReturn(NAME_PREFIX);
        when(identifierProperties.getSchemaUri()).thenReturn(SCHEMA_URI);

        final var id = idFactory.create(handle, servicePointRecord);

        assertThat(id, is(new Id()
                .id("%s%s/%s".formatted(NAME_PREFIX, prefix, suffix))
                .schemaUri(SCHEMA_URI)
                .registrationAgency(new RegistrationAgency()
                        .id(IDENTIFIER_REGISTRATION_AGENCY)
                        .schemaUri(rorSchemaUri))
                .owner(new Owner()
                        .id(IDENTIFIER_OWNER)
                        .schemaUri(rorSchemaUri)
                        .servicePoint(IDENTIFIER_SERVICE_POINT))
                .raidAgencyUrl("%s%s/%s".formatted(LANDING_PREFIX, prefix, suffix))
                .license(LICENSE)
                .version(1)));
    }
}