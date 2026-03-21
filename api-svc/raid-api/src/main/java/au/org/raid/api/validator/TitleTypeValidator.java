package au.org.raid.api.validator;

import au.org.raid.api.repository.TitleTypeRepository;
import au.org.raid.api.repository.TitleTypeSchemaRepository;
import au.org.raid.idl.raidv2.model.TitleType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
public class TitleTypeValidator {
    private final TitleTypeSchemaRepository titleTypeSchemaRepository;
    private final TitleTypeRepository titleTypeRepository;

    public TitleTypeValidator(final TitleTypeSchemaRepository titleTypeSchemaRepository, final TitleTypeRepository titleTypeRepository) {
        this.titleTypeSchemaRepository = titleTypeSchemaRepository;
        this.titleTypeRepository = titleTypeRepository;
    }

    public List<ValidationFailure> validate(final TitleType titleType, final int index) {
        final var failures = new ArrayList<ValidationFailure>();

        if (titleType == null) {
            return List.of(new ValidationFailure()
                    .fieldId("title[%d].type".formatted(index))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        }

        if (isBlank(titleType.getId())) {
            failures.add(new ValidationFailure()
                    .fieldId("title[%d].type.id".formatted(index))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        }

        if (isBlank(titleType.getSchemaUri())) {
            failures.add(new ValidationFailure()
                    .fieldId("title[%d].type.schemaUri".formatted(index))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        } else {
            final var titleTypeScheme =
                    titleTypeSchemaRepository.findActiveByUri(titleType.getSchemaUri());

            if (titleTypeScheme.isEmpty()) {
                failures.add(new ValidationFailure()
                        .fieldId("title[%d].type.schemaUri".formatted(index))
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_SCHEMA));
            } else if (!isBlank(titleType.getId()) &&
                    titleTypeRepository.findByUriAndSchemaId(titleType.getId(), titleTypeScheme.get().getId()).isEmpty()) {
                failures.add(new ValidationFailure()
                        .fieldId("title[%d].type.id".formatted(index))
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_ID_FOR_SCHEMA));
            }
        }

        return failures;
    }
}