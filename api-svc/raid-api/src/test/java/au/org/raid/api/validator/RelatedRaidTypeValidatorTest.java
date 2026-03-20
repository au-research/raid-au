package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedRaidTypeRepository;
import au.org.raid.api.repository.RelatedRaidTypeSchemaRepository;
import au.org.raid.api.util.TestConstants;
import au.org.raid.db.jooq.tables.records.RelatedRaidTypeRecord;
import au.org.raid.db.jooq.tables.records.RelatedRaidTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.RelatedRaidType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatedRaidTypeValidatorTest {
    private static final int INDEX = 3;
    private static final int RELATED_RAID_TYPE_SCHEMA_ID = 1;

    private static final RelatedRaidTypeSchemaRecord RELATED_RAID_TYPE_SCHEMA_RECORD = new RelatedRaidTypeSchemaRecord()
            .setId(RELATED_RAID_TYPE_SCHEMA_ID)
            .setUri(TestConstants.RELATED_RAID_TYPE_SCHEMA_URI);

    private static final RelatedRaidTypeRecord RELATED_RAID_TYPE_RECORD = new RelatedRaidTypeRecord()
            .setSchemaId(RELATED_RAID_TYPE_SCHEMA_ID)
            .setUri(TestConstants.CONTINUES_RELATED_RAID_TYPE);

    @Mock
    private RelatedRaidTypeSchemaRepository relatedRaidTypeSchemaRepository;
    @Mock
    private RelatedRaidTypeRepository relatedRaidTypeRepository;
    @InjectMocks
    private RelatedRaidTypeValidator validationService;


}