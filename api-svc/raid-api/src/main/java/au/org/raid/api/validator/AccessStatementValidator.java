package au.org.raid.api.validator;

import au.org.raid.idl.raidv2.model.AccessStatement;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_MESSAGE;
import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_TYPE;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
@RequiredArgsConstructor
public class AccessStatementValidator {
    private final LanguageValidator languageValidator;

    public List<ValidationFailure> validate(final AccessStatement accessStatement) {
        final var failures = new ArrayList<ValidationFailure>();

        if (accessStatement == null) {
            return List.of(new ValidationFailure()
                    .fieldId("access.statement")
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        }

        if (isBlank(accessStatement.getText())) {
            failures.add(new ValidationFailure()
                    .fieldId("access.statement.text")
                    .errorType(NOT_SET_TYPE)
                    .message(NOT_SET_MESSAGE)
            );
        }
        failures.addAll(
                languageValidator.validate(accessStatement.getLanguage(), "access.statement"));
        return failures;
    }
}
