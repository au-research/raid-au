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

        if (accessType == null) {
            return List.of(new ValidationFailure()
                    .fieldId("access.type")
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        }

        if (isBlank(accessType.getId())) {
            failures.add(new ValidationFailure()
                    .fieldId("access.type.id")
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        }

        if (isBlank(accessType.getSchemaUri())) {
            failures.add(new ValidationFailure()
                    .fieldId("access.type.schemaUri")
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        } else {
            final var accessTypeScheme =
                    accessTypeSchemaRepository.findActiveByUri(accessType.getSchemaUri());

            if (accessTypeScheme.isEmpty()) {
                failures.add(new ValidationFailure()
                        .fieldId("access.type.schemaUri")
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_SCHEMA));
            } else if (!isBlank(accessType.getId()) &&
                    accessTypeRepository.findByUriAndSchemaId(accessType.getId(), accessTypeScheme.get().getId()).isEmpty()) {
                failures.add(new ValidationFailure()
                        .fieldId("access.type.id")
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_ID_FOR_SCHEMA));
            }
        }

        return failures;
    }
}