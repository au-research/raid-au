package au.org.raid.api.validator;

import au.org.raid.api.client.ror.RorClient;
import au.org.raid.idl.raidv2.model.Organisation;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
@RequiredArgsConstructor
public class OrganisationValidator {
    // see https://ror.readme.io/docs/ror-identifier-pattern
    private final String regex = "^https://ror\\.org/[0-9a-z]{9}$";
    private static final String ROR_SCHEMA_URI = "https://ror.org/";
    private final OrganisationRoleValidator roleValidationService;
    private final RorClient rorClient;

    public List<ValidationFailure> validate(
            List<Organisation> organisations
    ) {

    /* organisations has been confirmed as optional in the metadata schema,
    rationale: an ORCID is quick to create (minutes), RORs can take months. */
        if (organisations == null) {
            return Collections.emptyList();
        }

        var failures = new ArrayList<ValidationFailure>();

        IntStream.range(0, organisations.size()).forEach(i -> {
            final var organisation = organisations.get(i);

            if (isBlank(organisation.getId())) {
                failures.add(new ValidationFailure()
                        .fieldId("organisation[%d].id".formatted(i))
                        .errorType(NOT_SET_TYPE)
                        .message(NOT_SET_MESSAGE)
                );
            } else {
                if (!organisation.getId().matches(regex)) {
                    failures.add(new ValidationFailure()
                            .fieldId("organisation[%d].id".formatted(i))
                            .errorType(INVALID_VALUE_TYPE)
                            .message(INVALID_VALUE_MESSAGE + " - should match %s".formatted(regex))
                    );

                } else if (!rorClient.exists(organisation.getId())) {
                        failures.add(new ValidationFailure()
                                .fieldId("organisation[%d].id".formatted(i))
                                .errorType(NOT_FOUND_TYPE)
                                .message("This ROR does not exist")
                        );
                    }
            }

            if (isBlank(organisation.getSchemaUri())) {
                failures.add(new ValidationFailure()
                        .fieldId("organisation[%d].schemaUri".formatted(i))
                        .errorType(NOT_SET_TYPE)
                        .message(NOT_SET_MESSAGE)
                );
            } else if (!organisation.getSchemaUri().equals(ROR_SCHEMA_URI)) {
                failures.add(new ValidationFailure()
                        .fieldId("organisation[%d].schemaUri")
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_VALUE_MESSAGE)
                );
            }

            IntStream.range(0, organisation.getRole().size()).forEach(roleIndex -> {
                final var role = organisation.getRole().get(roleIndex);
                failures.addAll(roleValidationService.validate(role, i, roleIndex));
            });
        });

        return failures;
    }
}

