package au.org.raid.api.validator;

import au.org.raid.api.repository.DescriptionTypeRepository;
import au.org.raid.api.repository.DescriptionTypeSchemaRepository;
import au.org.raid.api.util.TestConstants;
import au.org.raid.db.jooq.tables.records.DescriptionTypeRecord;
import au.org.raid.db.jooq.tables.records.DescriptionTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.DescriptionType;
import au.org.raid.idl.raidv2.model.DescriptionTypeIdEnum;
import au.org.raid.idl.raidv2.model.DescriptionTypeSchemaURIEnum;
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
class DescriptionTypeValidatorTest {
    private static final int INDEX = 3;
    private static final int DESCRIPTION_TYPE_SCHEMA_ID = 1;

    private static final DescriptionTypeSchemaRecord DESCRIPTION_TYPE_SCHEMA_RECORD = new DescriptionTypeSchemaRecord()
            .setId(DESCRIPTION_TYPE_SCHEMA_ID)
            .setUri(TestConstants.DESCRIPTION_TYPE_SCHEMA_URI);

    private static final DescriptionTypeRecord DESCRIPTION_TYPE_RECORD = new DescriptionTypeRecord()
            .setSchemaId(DESCRIPTION_TYPE_SCHEMA_ID)
            .setUri(TestConstants.PRIMARY_DESCRIPTION_TYPE);

    @Mock
    private DescriptionTypeSchemaRepository descriptionTypeSchemaRepository;
    @Mock
    private DescriptionTypeRepository descriptionTypeRepository;
    @InjectMocks
    private DescriptionTypeValidator validationService;


}