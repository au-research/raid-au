package au.org.raid.api.validator;

import au.org.raid.api.repository.ContributorRoleRepository;
import au.org.raid.api.repository.ContributorRoleSchemaRepository;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;

@Component
public class ContributorRoleValidator {
    private final ContributorRoleSchemaRepository contributorRoleSchemaRepository;
    private final ContributorRoleRepository contributorRoleRepository;

    public ContributorRoleValidator(final ContributorRoleSchemaRepository contributorRoleSchemaRepository, final ContributorRoleRepository contributorRoleRepository) {
        this.contributorRoleSchemaRepository = contributorRoleSchemaRepository;
        this.contributorRoleRepository = contributorRoleRepository;
    }

    public List<ValidationFailure> validate(
            final ContributorRole role, final int contributorIndex, final int roleIndex) {
        final var failures = new ArrayList<ValidationFailure>();

        if (role.getId() == null) {
            failures.add(
                    new ValidationFailure()
                            .fieldId("contributor[%d].role[%d].id".formatted(contributorIndex, roleIndex))
                            .errorType(NOT_SET_TYPE)
                            .message(NOT_SET_MESSAGE));
        }

        if (role.getSchemaUri() == null) {
            failures.add(
                    new ValidationFailure()
                            .fieldId("contributor[%d].role[%d].schemaUri".formatted(contributorIndex, roleIndex))
                            .errorType(NOT_SET_TYPE)
                            .message(NOT_SET_MESSAGE)
            );
        } else {
            final var roleScheme =
                    contributorRoleSchemaRepository.findActiveByUri(role.getSchemaUri().getValue());

            if (roleScheme.isEmpty()) {
                failures.add(
                        new ValidationFailure()
                                .fieldId("contributor[%d].role[%d].schemaUri".formatted(contributorIndex, roleIndex))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_SCHEMA)
                );
            } else if (role.getId() != null &&
                    contributorRoleRepository.findByUriAndSchemaId(role.getId().getValue(), roleScheme.get().getId()).isEmpty()) {
                failures.add(
                        new ValidationFailure()
                                .fieldId("contributor[%d].role[%d].id".formatted(contributorIndex, roleIndex))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_ID_FOR_SCHEMA)
                );
            }
        }

        return failures;
    }
}