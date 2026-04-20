package au.org.raid.api.validator;

import au.org.raid.api.repository.DescriptionTypeRepository;
import au.org.raid.api.repository.DescriptionTypeSchemaRepository;
import au.org.raid.idl.raidv2.model.DescriptionType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;

@Component
@RequiredArgsConstructor
public class DescriptionTypeValidator {
    private final DescriptionTypeSchemaRepository descriptionTypeSchemaRepository;
    private final DescriptionTypeRepository descriptionTypeRepository;

    public List<ValidationFailure> validate(final DescriptionType descriptionType, final int index) {
        final var failures = new ArrayList<ValidationFailure>();

        if (descriptionType == null) {
            return List.of(new ValidationFailure()
                    .fieldId("description[%d].type".formatted(index))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        }

        if (descriptionType.getId() == null) {
            failures.add(new ValidationFailure()
                    .fieldId("description[%d].type.id".formatted(index))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        }

        if (descriptionType.getSchemaUri() == null) {
            failures.add(new ValidationFailure()
                    .fieldId("description[%d].type.schemaUri".formatted(index))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        } else {
            final var descriptionTypeScheme =
                    descriptionTypeSchemaRepository.findActiveByUri(descriptionType.getSchemaUri().getValue());

            if (descriptionTypeScheme.isEmpty()) {
                failures.add(new ValidationFailure()
                        .fieldId("description[%d].type.schemaUri".formatted(index))
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_SCHEMA));
            } else if (descriptionType.getId() != null &&
                    descriptionTypeRepository.findByUriAndSchemaId(descriptionType.getId().getValue(), descriptionTypeScheme.get().getId()).isEmpty()) {
                failures.add(new ValidationFailure()
                        .fieldId("description[%d].type.id".formatted(index))
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_ID_FOR_SCHEMA));
            }
        }

        return failures;
    }
}