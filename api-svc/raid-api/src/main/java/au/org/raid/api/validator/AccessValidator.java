package au.org.raid.api.validator;

import au.org.raid.api.endpoint.message.ValidationMessage;
import au.org.raid.idl.raidv2.model.Access;
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
    private static final String ACCESS_TYPE_EMBARGOED =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/embargoed.json";
    private static final String ACCESS_TYPE_OPEN =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/open.json";
    private static final String ACCESS_TYPE_CLOSED =
            "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json";

    private final AccessTypeValidator typeValidationService;
    private final AccessStatementValidator accessStatementValidator;

    public List<ValidationFailure> validate(
            Access access
    ) {
        var failures = new ArrayList<ValidationFailure>();

        if (access == null) {
            failures.add(ValidationMessage.ACCESS_NOT_SET);
        } else if (access.getType() == null) {
            failures.add(
                    new ValidationFailure()
                            .errorType(NOT_SET_TYPE)
                            .fieldId("access.type")
                            .message(NOT_SET_MESSAGE)
            );
        } else {
            failures.addAll(typeValidationService.validate(access.getType()));

            if (!isBlank(access.getType().getId())) {
                final var typeId = access.getType().getId();

                if (typeId.equals(ACCESS_TYPE_CLOSED)) {
                    failures.add(new ValidationFailure()
                            .fieldId("access.type.id")
                            .errorType(INVALID_VALUE_TYPE)
                            .message("Creating closed Raids is no longer supported"));
                }

                if (typeId.equals(ACCESS_TYPE_EMBARGOED)) {
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
        }

        return failures;
    }
}