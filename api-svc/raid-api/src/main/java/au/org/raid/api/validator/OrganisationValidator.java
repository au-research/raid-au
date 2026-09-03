package au.org.raid.api.validator;

import au.org.raid.api.client.ror.RorClient;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.Organisation;
import au.org.raid.idl.raidv2.model.OrganisationRole;
import au.org.raid.idl.raidv2.model.UnavailableResolver;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.exception.ResolverUnavailableException.toUnavailableResolver;
import static au.org.raid.api.util.StringUtil.isBlank;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrganisationValidator {
    // see https://ror.readme.io/docs/ror-identifier-pattern
    private final String regex = "^https://ror\\.org/[0-9a-z]{9}$";
    private static final String ROR_SCHEMA_URI = "https://ror.org/";
    private final OrganisationRoleValidator roleValidationService;
    private final RorClient rorClient;

    public ValidationResult validate(
            List<Organisation> organisations
    ) {

    /* organisations has been confirmed as optional in the metadata schema,
    rationale: an ORCID is quick to create (minutes), RORs can take months. */
        if (organisations == null) {
            return ValidationResult.of(Collections.emptyList());
        }

        var failures = new ArrayList<ValidationFailure>();
        var unavailable = new ArrayList<UnavailableResolver>();

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

                } else {
                    try {
                        if (!rorClient.exists(organisation.getId())) {
                            failures.add(new ValidationFailure()
                                    .fieldId("organisation[%d].id".formatted(i))
                                    .errorType(NOT_FOUND_TYPE)
                                    .message("This ROR does not exist")
                            );
                        }
                    } catch (RestClientException e) {
                        // Covers ResourceAccessException (connect/read timeout, DNS failure,
                        // connection refused), HttpServerErrorException (5xx) and, defensively,
                        // any other RestClientException. HttpClientErrorException 404 is already
                        // handled inside RorClient.exists() (404 -> false), so only non-404
                        // failures reach here. The resolver, not the ROR, is at fault, so this is
                        // collected for a 503 (RAID-809) rather than treated as a validation
                        // failure of the organisation. Collect-then-throw so one organisation's
                        // resolver failure doesn't abort validation of the rest of the request.
                        log.error("External resolver check failed during ROR validation of {}", organisation.getId(), e);
                        unavailable.add(toUnavailableResolver(
                                "organisation[%d].id".formatted(i), organisation.getId(), "ROR", e));
                    }
                }
            }

            if (organisation.getSchemaUri() == null) {
                failures.add(new ValidationFailure()
                        .fieldId("organisation[%d].schemaUri".formatted(i))
                        .errorType(NOT_SET_TYPE)
                        .message(NOT_SET_MESSAGE)
                );
            } else if (!organisation.getSchemaUri().getValue().equals(ROR_SCHEMA_URI)) {
                failures.add(new ValidationFailure()
                        .fieldId("organisation[%d].schemaUri".formatted(i))
                        .errorType(INVALID_VALUE_TYPE)
                        .message(INVALID_VALUE_MESSAGE)
                );
            }

            if(DateRangeValidator.hasOverlaps(organisation.getRole(),
                    OrganisationRole::getStartDate,
                    OrganisationRole::getEndDate)) {
                failures.add(new ValidationFailure()
                        .fieldId("organisation[%d].role".formatted(i))
                        .errorType(INVALID_VALUE_TYPE)
                        .message("This contributor has simultaneous roles.")
                );
            }

            IntStream.range(0, organisation.getRole().size()).forEach(roleIndex -> {
                final var role = organisation.getRole().get(roleIndex);
                failures.addAll(roleValidationService.validate(role, i, roleIndex));
            });
        });

        final var organisationCountMap = organisations.stream()
                .filter(org -> org.getId() != null)
                .collect(Collectors.groupingBy(Organisation::getId, Collectors.counting()));

        for (final String id : organisationCountMap.keySet()) {
            final var occurrences = organisationCountMap.get(id);
            if (occurrences > 1) {
                failures.add(new ValidationFailure()
                        .fieldId("organisation")
                        .errorType(DUPLICATE_TYPE)
                        .message("An organisation can appear only once. There are %d occurrences of %s".formatted(occurrences, id))
                );
            }
        }

        return new ValidationResult(failures, unavailable);
    }
}

