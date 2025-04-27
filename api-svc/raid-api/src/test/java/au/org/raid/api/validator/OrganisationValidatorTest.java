package au.org.raid.api.validator;

import au.org.raid.api.util.TestConstants;
import au.org.raid.idl.raidv2.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_MESSAGE;
import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationValidatorTest {
    @Mock
    private OrganisationRoleValidator roleValidationService;

    @Mock
    private RorValidator rorValidationService;

    @InjectMocks
    private OrganisationValidator validationService;

    @Test
    @DisplayName("Validation passes with valid organisation")
    void validOrganisation() {
        final var role = new OrganisationRole()
                .schemaUri(OrganizationRoleSchemaUriEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_359)
                .id(OrganizationRoleIdEnum.HTTPS_VOCABULARY_RAID_ORG_ORGANISATION_ROLE_SCHEMA_182)
                .startDate(LocalDate.now().minusYears(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .endDate(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        final var organisation = new Organisation()
                .id(TestConstants.VALID_ROR)
                .schemaUri(OrganizationSchemaUriEnum.HTTPS_ROR_ORG_)
                .role(List.of(role));

        final var failures = validationService.validate(List.of(organisation));

        assertThat(failures, empty());
        verify(rorValidationService).validate(TestConstants.VALID_ROR, 0);
        verify(roleValidationService).validate(role, 0, 0);
    }

}