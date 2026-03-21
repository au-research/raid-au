package au.org.raid.api.validator;

import au.org.raid.api.util.SchemaValues;
import au.org.raid.idl.raidv2.model.Description;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
@RequiredArgsConstructor
public class DescriptionValidator {

    private final DescriptionTypeValidator typeValidationService;
    private final LanguageValidator languageValidator;

    public List<ValidationFailure> validate(
            List<Description> descriptions
    ) {
        if (descriptions == null || descriptions.isEmpty()) {
            // allowed to have no desc, not sure if parser will pass through null
            // or empty if property is not set at all - either way, it's allowed
            return Collections.emptyList();
        }

        var failures = new ArrayList<ValidationFailure>();

        final var primaryDescriptions = descriptions.stream()
                .filter(description -> description.getType() != null)
                .filter(description -> description.getType().getId() != null)
                .filter(description -> description.getType().getId().equals(SchemaValues.PRIMARY_DESCRIPTION_TYPE.getUri()))
                .toList();

        if (primaryDescriptions.isEmpty()) {
            failures.add(new ValidationFailure()
                    .fieldId("description")
                    .errorType(INVALID_VALUE_TYPE)
                    .message("Descriptions are optional but if specified one must be the primary description.")
            );
        } else if (primaryDescriptions.size() > 1) {
            final var primaryPositions = new ArrayList<String>();

            IntStream.range(0, descriptions.size())
                    .forEach(i -> {
                        final var description = descriptions.get(i);
                        if (description.getType() != null &&
                                description.getType().getId().equals(SchemaValues.PRIMARY_DESCRIPTION_TYPE.getUri())) {
                            primaryPositions.add("description[%d].type.id".formatted(i));
                        }
                    });

            failures.add(new ValidationFailure()
                    .fieldId(primaryPositions.remove(0))
                    .errorType(INVALID_VALUE_TYPE)
                    .message("There can only be one primary description. This description conflicts with %s"
                            .formatted(String.join(", ", primaryPositions)))
            );
        }

        IntStream.range(0, descriptions.size()).forEach(index -> {
            final var description = descriptions.get(index);

            if (isBlank(description.getText())) {
                failures.add(new ValidationFailure()
                        .fieldId("description[%d].text".formatted(index))
                        .errorType(NOT_SET_TYPE)
                        .message(NOT_SET_MESSAGE)
                );
            }

            failures.addAll(typeValidationService.validate(description.getType(), index));
            failures.addAll(
                    languageValidator.validate(description.getLanguage(), "description[%d]".formatted(index))
            );
        });

        return failures;
    }
}