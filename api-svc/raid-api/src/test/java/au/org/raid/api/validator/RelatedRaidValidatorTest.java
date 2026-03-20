package au.org.raid.api.validator;

import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static au.org.raid.api.util.TestConstants.DESCRIPTION_TYPE_SCHEMA_URI;
import static au.org.raid.api.util.TestConstants.PRIMARY_DESCRIPTION_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class RelatedRaidValidatorTest {
    private static final String ID = "https://raid.org/10.121221/73864387";

    @Mock
    private RelatedRaidTypeValidator typeValidationService;

    @InjectMocks
    private RelatedRaidValidator validationService;

    @Test
    @DisplayName("Validation passes with valid related raid")
    void validRelatedRaid() {
        final var type = new RelatedRaidType()
                .id(RelatedRaidTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_198)
                .schemaUri(RelatedRaidTypeSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_RELATED_RAID_TYPE_SCHEMA_367);

        final var relatedRaid = new RelatedRaid()
                .id(ID)
                .type(type);

        final var failures = validationService.validate(List.of(relatedRaid));

        assertThat(failures, empty());

        verify(typeValidationService).validate(type, 0);
    }

}