package au.org.raid.api.validator;

import au.org.raid.api.repository.RelatedObjectCategoryRepository;
import au.org.raid.api.repository.RelatedObjectCategorySchemaRepository;
import au.org.raid.idl.raidv2.model.RelatedObjectCategory;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
public class RelatedObjectCategoryValidator {
    private final RelatedObjectCategoryRepository relatedObjectCategoryRepository;
    private final RelatedObjectCategorySchemaRepository relatedObjectCategorySchemaRepository;

    public RelatedObjectCategoryValidator(final RelatedObjectCategoryRepository relatedObjectCategoryRepository, final RelatedObjectCategorySchemaRepository relatedObjectCategorySchemaRepository) {
        this.relatedObjectCategoryRepository = relatedObjectCategoryRepository;
        this.relatedObjectCategorySchemaRepository = relatedObjectCategorySchemaRepository;
    }

    public List<ValidationFailure> validate(final List<RelatedObjectCategory> categories, final int index) {
        var failures = new ArrayList<ValidationFailure>();

        IntStream.range(0, categories.size()).forEach(i -> {
            final var relatedObjectCategory = categories.get(i);

            if (isBlank(relatedObjectCategory.getId())) {
                failures.add(new ValidationFailure()
                        .fieldId("relatedObject[%d].category[%d].id".formatted(index, i))
                        .errorType(NOT_SET_TYPE)
                        .message(NOT_SET_MESSAGE)
                );
            }
            if (isBlank(relatedObjectCategory.getSchemaUri())) {
                failures.add(new ValidationFailure()
                        .fieldId("relatedObject[%d].category[%d].schemaUri".formatted(index, i))
                        .errorType(NOT_SET_TYPE)
                        .message(NOT_SET_MESSAGE)
                );
            } else {
                final var relatedObjectCategoryScheme =
                        relatedObjectCategorySchemaRepository.findActiveByUri(relatedObjectCategory.getSchemaUri());

                if (relatedObjectCategoryScheme.isEmpty()) {
                    failures.add(new ValidationFailure()
                            .fieldId("relatedObject[%d].category[%d].schemaUri".formatted(index, i))
                            .errorType(INVALID_VALUE_TYPE)
                            .message(INVALID_SCHEMA)
                    );
                } else if (!isBlank(relatedObjectCategory.getId()) &&
                        relatedObjectCategoryRepository.findByUriAndSchemaId(relatedObjectCategory.getId(), relatedObjectCategoryScheme.get().getId()).isEmpty()) {
                    failures.add(new ValidationFailure()
                            .fieldId("relatedObject[%d].category[%d].id".formatted(index, i))
                            .errorType(INVALID_VALUE_TYPE)
                            .message(INVALID_ID_FOR_SCHEMA)
                    );
                }
            }
        });

        return failures;
    }
}
