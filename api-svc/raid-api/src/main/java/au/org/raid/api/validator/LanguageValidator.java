package au.org.raid.api.validator;

import au.org.raid.api.repository.LanguageRepository;
import au.org.raid.api.repository.LanguageSchemaRepository;
import au.org.raid.idl.raidv2.model.Language;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
@RequiredArgsConstructor
public class LanguageValidator {
    private final LanguageSchemaRepository languageSchemaRepository;
    private final LanguageRepository languageRepository;

    public List<ValidationFailure> validate(final Language language, final String parent) {
        final var failures = new ArrayList<ValidationFailure>();

        if (language == null) {
            return failures;
        }

        if (isBlank(language.getId())) {
            failures.add(new ValidationFailure()
                    .fieldId("%s.language.id".formatted(parent))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        }
        if (isBlank(language.getSchemaUri())) {
            failures.add(new ValidationFailure()
                    .fieldId("%s.language.schemaUri".formatted(parent))
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        } else {
            final var languageScheme = languageSchemaRepository.findActiveByUri(language.getSchemaUri());

            if (languageScheme.isEmpty()) {
                failures.add(new ValidationFailure()
                        .fieldId("%s.language.schemaUri".formatted(parent))
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_SCHEMA)
                );
            } else if (!isBlank(language.getId()) &&
                    languageRepository.findByIdAndSchemaId(language.getId(), languageScheme.get().getId()).isEmpty()) {
                failures.add(new ValidationFailure()
                        .fieldId("%s.language.id".formatted(parent))
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_ID_FOR_SCHEMA)
                );


            }
        }

        return failures;
    }
}
