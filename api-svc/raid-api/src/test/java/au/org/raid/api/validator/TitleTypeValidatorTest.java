package au.org.raid.api.validator;

import au.org.raid.api.repository.TitleTypeRepository;
import au.org.raid.api.repository.TitleTypeSchemaRepository;
import au.org.raid.api.util.TestConstants;
import au.org.raid.db.jooq.tables.records.TitleTypeRecord;
import au.org.raid.db.jooq.tables.records.TitleTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.TitleType;
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
class TitleTypeValidatorTest {
    private static final int INDEX = 3;
    private static final int TITLE_TYPE_SCHEMA_ID = 1;

    private static final TitleTypeSchemaRecord TITLE_TYPE_SCHEMA_RECORD = new TitleTypeSchemaRecord()
            .setId(TITLE_TYPE_SCHEMA_ID)
            .setUri(TestConstants.TITLE_TYPE_SCHEMA_URI);

    private static final TitleTypeRecord TITLE_TYPE_RECORD = new TitleTypeRecord()
            .setSchemaId(TITLE_TYPE_SCHEMA_ID)
            .setUri(TestConstants.PRIMARY_TITLE_TYPE_ID);

    @Mock
    private TitleTypeSchemaRepository titleTypeSchemaRepository;
    @Mock
    private TitleTypeRepository titleTypeRepository;
    @InjectMocks
    private TitleTypeValidator validationService;



}