package au.org.raid.api.service;

import au.org.raid.api.exception.AccessTypeNotFoundException;
import au.org.raid.api.exception.AccessTypeSchemaNotFoundException;
import au.org.raid.api.factory.AccessFactory;
import au.org.raid.api.factory.AccessStatementFactory;
import au.org.raid.api.repository.AccessTypeRepository;
import au.org.raid.api.repository.AccessTypeSchemaRepository;
import au.org.raid.api.util.TestRaid;
import au.org.raid.db.jooq.tables.records.AccessTypeRecord;
import au.org.raid.db.jooq.tables.records.AccessTypeSchemaRecord;
import au.org.raid.db.jooq.tables.records.RaidRecord;
import au.org.raid.idl.raidv2.model.Access;
import au.org.raid.idl.raidv2.model.AccessStatement;
import au.org.raid.idl.raidv2.model.AccessType;
import au.org.raid.idl.raidv2.model.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {
    @Mock
    private AccessTypeSchemaRepository accessTypeSchemaRepository;
    @Mock
    private AccessTypeRepository accessTypeRepository;
    @Mock
    private AccessFactory accessFactory;
    @Mock
    private LanguageService languageService;
    @Mock
    private AccessTypeService accessTypeService;
    @Mock
    private AccessStatementFactory accessStatementFactory;
    @InjectMocks
    private AccessService accessService;

    @Test
    @DisplayName("findAccessTypeId return access type id from Access")
    void findAccessTypeId() {
        final var access = TestRaid.RAID_DTO.getAccess();
        final var accessTypeSchemaId = 123;
        final var accessTypeRecordId = 234;

        final var accessTypeSchemaRecord = new AccessTypeSchemaRecord().setId(accessTypeSchemaId);
        final var accessTypeRecord = new AccessTypeRecord().setId(accessTypeRecordId);

        when(accessTypeSchemaRepository.findByUri(TestRaid.ACCESS_TYPE_SCHEMA_URI))
                .thenReturn(Optional.of(accessTypeSchemaRecord));
        when(accessTypeRepository.findByUriAndSchemaId(TestRaid.ACCESS_TYPE_ID, accessTypeSchemaId))
                .thenReturn(Optional.of(accessTypeRecord));

        final var result = accessService.findAccessTypeId(access);

        assertThat(result, is(accessTypeRecordId));
    }

    @Test
    @DisplayName("findAccessTypeId throws AccessTypeNotFoundException if access type not found")
    void throwsAccessTypeNotFound() {
        final var access = TestRaid.RAID_DTO.getAccess();
        final var accessTypeSchemaId = 123;
        final var accessTypeRecordId = 234;

        final var accessTypeSchemaRecord = new AccessTypeSchemaRecord().setId(accessTypeSchemaId);

        when(accessTypeSchemaRepository.findByUri(TestRaid.ACCESS_TYPE_SCHEMA_URI))
                .thenReturn(Optional.of(accessTypeSchemaRecord));
        when(accessTypeRepository.findByUriAndSchemaId(TestRaid.ACCESS_TYPE_ID, accessTypeSchemaId))
                .thenReturn(Optional.empty());

        assertThrows(AccessTypeNotFoundException.class, () -> accessService.findAccessTypeId(access));
    }


    @Test
    @DisplayName("findAccessTypeId throws AccessTypeSchemaNotFoundException if access type schema not found")
    void throwsAccessTypeSchemaNotFoundException() {
        final var access = TestRaid.RAID_DTO.getAccess();

        when(accessTypeSchemaRepository.findByUri(TestRaid.ACCESS_TYPE_SCHEMA_URI))
                .thenReturn(Optional.empty());

        assertThrows(AccessTypeSchemaNotFoundException.class, () -> accessService.findAccessTypeId(access));
    }

    @Test
    @DisplayName("getAccess() returns Access from RaidRecord")
    void getAccess() {
        final var languageId = 123;
        final var accessTypeId = 234;
        final var accessStatementText = "access-statement";
        final var embargoExpiry = LocalDate.now();

        final var record = new RaidRecord()
                .setAccessStatementLanguageId(languageId)
                .setAccessTypeId(accessTypeId)
                .setAccessStatement(accessStatementText)
                .setEmbargoExpiry(embargoExpiry);

        final var language = new Language();
        final var accessType = new AccessType();
        final var accessStatement = new AccessStatement();
        final var access = new Access();

        when(languageService.findById(languageId)).thenReturn(language);
        when(accessTypeService.findById(accessTypeId)).thenReturn(accessType);
        when(accessStatementFactory.create(accessStatementText, language)).thenReturn(accessStatement);
        when(accessFactory.create(accessStatement, accessType, embargoExpiry)).thenReturn(access);

        assertThat(accessService.getAccess(record), is(access));
    }
}
