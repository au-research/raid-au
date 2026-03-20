package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedObjectTypeRepository;
import au.org.raid.api.repository.RelatedObjectTypeSchemaRepository;
import au.org.raid.api.util.TestConstants;
import au.org.raid.db.jooq.tables.records.RelatedObjectTypeRecord;
import au.org.raid.db.jooq.tables.records.RelatedObjectTypeSchemaRecord;
import au.org.raid.idl.raidv2.model.RelatedObjectType;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatedObjectTypeValidatorTest {
    private static final int INDEX = 3;
    private static final int RELATED_OBJECT_TYPE_SCHEMA_ID = 1;

    private static final RelatedObjectTypeSchemaRecord RELATED_OBJECT_TYPE_SCHEMA_RECORD =
            new RelatedObjectTypeSchemaRecord()
                    .setId(RELATED_OBJECT_TYPE_SCHEMA_ID)
                    .setUri(TestConstants.RELATED_OBJECT_TYPE_SCHEMA_URI);

    private static final RelatedObjectTypeRecord RELATED_OBJECT_TYPE_RECORD =
            new RelatedObjectTypeRecord()
                    .setSchemaId(RELATED_OBJECT_TYPE_SCHEMA_ID)
                    .setUri(TestConstants.BOOK_CHAPTER_RELATED_OBJECT_TYPE);

    @Mock
    private RelatedObjectTypeRepository relatedObjectTypeRepository;

    @Mock
    private RelatedObjectTypeSchemaRepository relatedObjectTypeSchemaRepository;

    @InjectMocks
    private RelatedObjectTypeValidator validationService;

}