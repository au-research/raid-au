package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedObjectTypeRepository;
import au.org.raid.api.repository.RelatedObjectTypeSchemaRepository;
import au.org.raid.idl.raidv2.model.RelatedObjectType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;

@Component
public class RelatedObjectTypeValidator {
    private final RelatedObjectTypeRepository relatedObjectTypeRepository;
    private final RelatedObjectTypeSchemaRepository relatedObjectTypeSchemaRepository;

    public RelatedObjectTypeValidator(final RelatedObjectTypeRepository relatedObjectTypeRepository, final RelatedObjectTypeSchemaRepository relatedObjectTypeSchemaRepository) {
        this.relatedObjectTypeRepository = relatedObjectTypeRepository;
        this.relatedObjectTypeSchemaRepository = relatedObjectTypeSchemaRepository;
    }


    public List<ValidationFailure> validate(final RelatedObjectType relatedObjectType, final int index) {
        var failures = new ArrayList<ValidationFailure>();

        if (relatedObjectType.getId() == null) {
            failures.add(new ValidationFailure()
                    .fieldId("relatedObject[%d].type.id".formatted(index))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        }
        if (relatedObjectType.getSchemaUri() == null) {
            failures.add(new ValidationFailure()
                    .fieldId("relatedObject[%d].type.schemaUri".formatted(index))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        } else {
            final var relatedObjectTypeScheme =
                    relatedObjectTypeSchemaRepository.findActiveByUri(relatedObjectType.getSchemaUri().getValue());

            if (relatedObjectTypeScheme.isEmpty()) {
                failures.add(new ValidationFailure()
                        .fieldId("relatedObject[%d].type.schemaUri".formatted(index))
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_SCHEMA)
                );
            } else if (relatedObjectType.getId() != null &&
                    relatedObjectTypeRepository.findByUriAndSchemaId(relatedObjectType.getId().getValue(), relatedObjectTypeScheme.get().getId()).isEmpty()) {
                failures.add(new ValidationFailure()
                        .fieldId("relatedObject[%d].type.id".formatted(index))
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_ID_FOR_SCHEMA)
                );
            }
        }

        return failures;
    }
}
