package au.org.raid.api.validator;

import au.org.raid.api.repository.ContributorRoleRepository;
import au.org.raid.api.repository.ContributorRoleSchemaRepository;
import au.org.raid.idl.raidv2.model.ContributorRole;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

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

        return failures;
    }
}