package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedRaidTypeRepository;
import au.org.raid.api.repository.RelatedRaidTypeSchemaRepository;
import au.org.raid.idl.raidv2.model.RelatedRaidType;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
@RequiredArgsConstructor
public class RelatedRaidTypeValidator {
    private final RelatedRaidTypeSchemaRepository relatedRaidTypeSchemaRepository;
    private final RelatedRaidTypeRepository relatedRaidTypeRepository;

    public List<ValidationFailure> validate(final RelatedRaidType relatedRaidType, final int index) {
        final var failures = new ArrayList<ValidationFailure>();

        if (relatedRaidType == null) {
            return List.of(new ValidationFailure()
                    .fieldId("relatedRaid[%d].type".formatted(index))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        }

        if (isBlank(relatedRaidType.getId())) {
            failures.add(new ValidationFailure()
                    .fieldId("relatedRaid[%d].type.id".formatted(index))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        }

        if (isBlank(relatedRaidType.getSchemaUri())) {
            failures.add(new ValidationFailure()
                    .fieldId("relatedRaid[%d].type.schemaUri".formatted(index))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        } else {
            final var relatedRaidTypeScheme =
                    relatedRaidTypeSchemaRepository.findActiveByUri(relatedRaidType.getSchemaUri());

            if (relatedRaidTypeScheme.isEmpty()) {
                failures.add(new ValidationFailure()
                        .fieldId("relatedRaid[%d].type.schemaUri".formatted(index))
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_SCHEMA));
            } else if (!isBlank(relatedRaidType.getId()) &&
                    relatedRaidTypeRepository.findByUriAndSchemaId(relatedRaidType.getId(), relatedRaidTypeScheme.get().getId()).isEmpty()) {
                failures.add(new ValidationFailure()
                        .fieldId("relatedRaid[%d].type.id".formatted(index))
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_ID_FOR_SCHEMA));
            }
        }

        return failures;
    }

}