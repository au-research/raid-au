package au.org.raid.api.validator;

import au.org.raid.api.repository.AccessTypeRepository;
import au.org.raid.api.repository.AccessTypeSchemaRepository;
import au.org.raid.idl.raidv2.model.AccessType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
public class AccessTypeValidator {
    private final AccessTypeSchemaRepository accessTypeSchemaRepository;
    private final AccessTypeRepository accessTypeRepository;

    public AccessTypeValidator(final AccessTypeSchemaRepository accessTypeSchemaRepository, final AccessTypeRepository accessTypeRepository) {
        this.accessTypeSchemaRepository = accessTypeSchemaRepository;
        this.accessTypeRepository = accessTypeRepository;
    }

    public List<ValidationFailure> validate(final AccessType accessType) {
        final var failures = new ArrayList<ValidationFailure>();

        return failures;
    }
}