package au.org.raid.api.validator;

import au.org.raid.api.endpoint.message.ValidationMessage;
import au.org.raid.idl.raidv2.model.Access;
import au.org.raid.idl.raidv2.model.AccessTypeIdEnum;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
@RequiredArgsConstructor
public class AccessValidator {
    private static final String ACCESS_TYPE_CLOSED =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json";

    private final AccessTypeValidator typeValidationService;
    private final AccessStatementValidator accessStatementValidator;

    public List<ValidationFailure> validate(
            Access access
    ) {
        var failures = new ArrayList<ValidationFailure>();

            failures.addAll(typeValidationService.validate(access.getType()));

            if (!isBlank(access.getType().getId().getValue())) {
                final var typeId = access.getType().getId();

                if (typeId.equals(AccessTypeIdEnum.HTTPS_VOCABULARIES_COAR_REPOSITORIES_ORG_ACCESS_RIGHTS_C_F1CF_)) {
                    if (access.getStatement() == null) {
                        failures.add(new ValidationFailure()
                                .fieldId("access.statement")
                                .errorType(NOT_SET_TYPE)
                                .message(NOT_SET_MESSAGE));
                    }
                    if(access.getEmbargoExpiry() == null) {
                        failures.add(new ValidationFailure()
                                .errorType(NOT_SET_TYPE)
                                .fieldId("access.embargoExpiry")
                                .message(NOT_SET_MESSAGE));
                    } else if (access.getEmbargoExpiry().isAfter(LocalDate.now().plusMonths(18))) {
                        failures.add(new ValidationFailure()
                                .fieldId("access.embargoExpiry")
                                .errorType(INVALID_VALUE_TYPE)
                                .message("Embargo expiry cannot be more than 18 months in the future"));
                    }
                }
            }
            if (access.getStatement() != null) {
                failures.addAll(
                        accessStatementValidator.validate(access.getStatement())
                );
            }


        return failures;
    }
}