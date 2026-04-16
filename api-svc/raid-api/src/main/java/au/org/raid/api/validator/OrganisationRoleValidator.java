package au.org.raid.api.validator;

import au.org.raid.api.repository.OrganisationRoleRepository;
import au.org.raid.api.repository.OrganisationRoleSchemaRepository;
import au.org.raid.api.util.DateUtil;
import au.org.raid.idl.raidv2.model.OrganisationRole;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
public class OrganisationRoleValidator {
    private final OrganisationRoleSchemaRepository organisationRoleSchemaRepository;
    private final OrganisationRoleRepository organisationRoleRepository;

    public OrganisationRoleValidator(final OrganisationRoleSchemaRepository organisationRoleSchemaRepository, final OrganisationRoleRepository organisationRoleRepository) {
        this.organisationRoleSchemaRepository = organisationRoleSchemaRepository;
        this.organisationRoleRepository = organisationRoleRepository;
    }

    public List<ValidationFailure> validate(
            final OrganisationRole role, final int organisationIndex, final int roleIndex) {
        final var failures = new ArrayList<ValidationFailure>();

        if (isBlank(role.getId())) {
            failures.add(
                    new ValidationFailure()
                            .fieldId("organisation[%d].role[%d].id".formatted(organisationIndex, roleIndex))
                            .errorType(NOT_SET_TYPE)
                            .message(NOT_SET_MESSAGE));
        }

        if (isBlank(role.getSchemaUri())) {
            failures.add(
                    new ValidationFailure()
                            .fieldId("organisation[%d].role[%d].schemaUri".formatted(organisationIndex, roleIndex))
                            .errorType(NOT_SET_TYPE)
                            .message(NOT_SET_MESSAGE)
            );
        } else {
            final var roleScheme =
                    organisationRoleSchemaRepository.findActiveByUri(role.getSchemaUri());

            if (roleScheme.isEmpty()) {
                failures.add(
                        new ValidationFailure()
                                .fieldId("organisation[%d].role[%d].schemaUri".formatted(organisationIndex, roleIndex))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_SCHEMA)
                );
            } else if (!isBlank(role.getId()) &&
                    organisationRoleRepository.findByUriAndSchemaId(role.getId(), roleScheme.get().getId()).isEmpty()) {
                failures.add(
                        new ValidationFailure()
                                .fieldId("organisation[%d].role[%d].id".formatted(organisationIndex, roleIndex))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_ID_FOR_SCHEMA)
                );
            }
        }

        if (isBlank(role.getStartDate())) {
            failures.add(new ValidationFailure()
                    .fieldId("organisation[%d].role[%d].startDate".formatted(organisationIndex, roleIndex))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        } else if (!isBlank(role.getEndDate()) && DateUtil.parseDate(role.getEndDate()).isBefore(DateUtil.parseDate(role.getStartDate()))) {
            failures.add(new ValidationFailure()
                    .fieldId("organisation[%d].role[%d].endDate".formatted(organisationIndex, roleIndex))
                    .errorType(INVALID_VALUE_TYPE)
                    .message(END_DATE_BEFORE_START_DATE)
            );
        }

        return failures;
    }
}