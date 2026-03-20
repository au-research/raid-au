package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedObjectCategoryRepository;
import au.org.raid.api.repository.RelatedObjectCategorySchemaRepository;
import au.org.raid.api.util.TestConstants;
import au.org.raid.db.jooq.tables.records.RelatedObjectCategoryRecord;
import au.org.raid.db.jooq.tables.records.RelatedObjectCategorySchemaRecord;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatedObjectCategoryValidatorTest {
    private static final int INDEX = 3;
    private static final int RELATED_OBJECT_CATEGORY_SCHEMA_ID = 1;

    private static final RelatedObjectCategorySchemaRecord RELATED_OBJECT_CATEGORY_SCHEMA_RECORD =
            new RelatedObjectCategorySchemaRecord()
                    .setId(RELATED_OBJECT_CATEGORY_SCHEMA_ID)
                    .setUri(TestConstants.RELATED_OBJECT_CATEGORY_SCHEMA_URI);

    private static final RelatedObjectCategoryRecord RELATED_OBJECT_CATEGORY_RECORD =
            new RelatedObjectCategoryRecord()
                    .setSchemaId(RELATED_OBJECT_CATEGORY_SCHEMA_ID)
                    .setUri(TestConstants.INPUT_RELATED_OBJECT_CATEGORY);

    @Mock
    private RelatedObjectCategoryRepository relatedObjectCategoryRepository;

    @Mock
    private RelatedObjectCategorySchemaRepository relatedObjectCategorySchemaRepository;

    @InjectMocks
    private RelatedObjectCategoryValidator validationService;

}