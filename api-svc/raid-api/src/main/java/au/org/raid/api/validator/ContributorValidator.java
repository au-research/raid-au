package au.org.raid.api.validator;

import au.org.raid.api.config.properties.ContributorValidationProperties;
import au.org.raid.api.dto.ContributorStatus;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.api.util.DateUtil;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.Organisation;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
@RequiredArgsConstructor
public class ContributorValidator {
    private final ContributorRepository contributorRepository;
    private final ContributorTypeValidator isniValidator;
    private final ContributorTypeValidator orcidValidator;
    private final ContributorValidationProperties validationProperties;
    private final ContributorRoleValidator roleValidator;
    private final ContributorPositionValidator positionValidator;

    public List<ValidationFailure> validate(
            List<Contributor> contributors
    ) {
        if (contributors == null || contributors.isEmpty()) {
            return List.of(CONTRIB_NOT_SET);
        }

        var failures = new ArrayList<ValidationFailure>();

        final var contributorCountMap = contributors.stream()
                .filter(org -> org.getId() != null)
                .collect(Collectors.groupingBy(Contributor::getId, Collectors.counting()));

        for (final String id : contributorCountMap.keySet()) {
            final var occurrences = contributorCountMap.get(id);
            if (occurrences > 1) {
                failures.add(new ValidationFailure()
                        .fieldId("contributor[1].id")
                        .errorType(DUPLICATE_TYPE)
                        .message("A contributor can appear only once. There are %d occurrences of %s".formatted(occurrences, id))
                );
            }
        }


        IntStream.range(0, contributors.size())
                .forEach(index -> {
                    final var contributor = contributors.get(index);

                    if (isOrcid(contributor)) {
                        failures.addAll(orcidValidator.validate(contributor, index));
                    } else if (isIsni(contributor)) {
                        failures.addAll(isniValidator.validate(contributor, index));
                    } else {

                        failures.add(new ValidationFailure()
                                .fieldId("contributor[%d].id".formatted(index))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_VALUE_MESSAGE + " - should begin with %s or %s".formatted(
                                        validationProperties.getOrcid().getUrlPrefix(),
                                        validationProperties.getIsni().getUrlPrefix()
                                ))
                        );
                        failures.add(new ValidationFailure()
                                .fieldId("contributor[%d].schemaUri".formatted(index))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_VALUE_MESSAGE + " - should be %s or %s".formatted(
                                        validationProperties.getOrcid().getSchemaUri(),
                                        validationProperties.getIsni().getSchemaUri()
                                ))
                        );
                    }
                });

        failures.addAll(validateLeader(contributors));
        failures.addAll(validateContact(contributors));

        return failures;
    }

    private boolean isOrcid(final Contributor contributor) {
        if (contributor.getId() != null && contributor.getId().startsWith(validationProperties.getOrcid().getUrlPrefix())) {
            return true;
        }
        if (contributor.getSchemaUri() != null && contributor.getSchemaUri().equals(validationProperties.getOrcid().getSchemaUri())) {
            return true;
        }
        return false;
    }

    private boolean isIsni(final Contributor contributor) {
        if (contributor.getId() != null && contributor.getId().startsWith(validationProperties.getIsni().getUrlPrefix())) {
            return true;
        }
        if (contributor.getSchemaUri() != null && contributor.getSchemaUri().equals(validationProperties.getIsni().getSchemaUri())) {
            return true;
        }
        return false;
    }

    public List<ValidationFailure> validateForPatch(
            List<Contributor> contributors
    ) {
        if (contributors == null || contributors.isEmpty()) {
            return List.of(CONTRIB_NOT_SET);
        }

        var failures = new ArrayList<ValidationFailure>();

        IntStream.range(0, contributors.size())
                .forEach(index -> {
                    final var contributor = contributors.get(index);

                    final var isValid = Arrays.stream(ContributorStatus.values())
                            .anyMatch(value -> value.name().equals(contributor.getStatus().toUpperCase()));

                    if (!isValid) {
                        failures.add(
                                new ValidationFailure()
                                        .fieldId("contributor[%d].status".formatted(index))
                                        .errorType(INVALID_VALUE_TYPE)
                                        .message("Contributor status should be one of %s"
                                                .formatted(Arrays.stream(ContributorStatus.values())
                                                        .map(Enum::name)
                                                        .collect(Collectors.joining(", ")))
                                        )
                        );
                    }
                    if (isOrcid(contributor)) {
                        failures.addAll(orcidValidator.validate(contributor, index));
                    } else if (isIsni(contributor)) {
                        failures.addAll(isniValidator.validate(contributor, index));
                    } else {
                        failures.add(new ValidationFailure()
                                .fieldId("contributor[%d].id".formatted(index))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_VALUE_MESSAGE + " - should begin with %s or %s".formatted(
                                        validationProperties.getOrcid().getUrlPrefix(),
                                        validationProperties.getIsni().getUrlPrefix()
                                ))
                        );
                        failures.add(new ValidationFailure()
                                .fieldId("contributor[%d].schemaUri".formatted(index))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_VALUE_MESSAGE + " - should be %s or %s".formatted(
                                        validationProperties.getOrcid().getSchemaUri(),
                                        validationProperties.getIsni().getSchemaUri()
                                ))
                        );
                    }
                });

        failures.addAll(validateLeader(contributors));
        failures.addAll(validateContact(contributors));

        return failures;
    }

    private List<ValidationFailure> validateLeader(
            List<Contributor> contributors
    ) {
        var failures = new ArrayList<ValidationFailure>();

        var leaders = contributors.stream()
                .filter(contributor -> contributor.getLeader() != null && contributor.getLeader())
                .toList();

        if (leaders.isEmpty()) {
            failures.add(new ValidationFailure().
                    fieldId("contributor").
                    errorType(NOT_SET_TYPE).
                    message("At least one contributor must be flagged as a project leader"));
        }

        return failures;
    }

    private List<ValidationFailure> validateContact(
            List<Contributor> contributors
    ) {
        var failures = new ArrayList<ValidationFailure>();

        var leaders = contributors.stream()
                .filter(contributor -> contributor.getContact() != null && contributor.getContact())
                .toList();

        if (leaders.isEmpty()) {
            failures.add(new ValidationFailure().
                    fieldId("contributor").
                    errorType(NOT_SET_TYPE).
                    message("At least one contributor must be flagged as a project contact"));
        }

        return failures;
    }

    private List<ValidationFailure> validatePositions(final List<ContributorPosition> positions, final int contributorIndex) {
        final var failures = new ArrayList<ValidationFailure>();
        var sortedPositions = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < positions.size(); i++) {
            final var position = positions.get(i);

            sortedPositions.add(Map.of(
                    "index", i,
                    "start", DateUtil.parseDate(position.getStartDate()),
                    "end", position.getEndDate() == null ? LocalDate.now() : DateUtil.parseDate(position.getEndDate())
                    ));
        }

        sortedPositions.sort((o1, o2) -> {
            if (o1.get("start").equals(o2.get("start"))) {
                return ((LocalDate) o1.get("end")).compareTo((LocalDate) o2.get("end"));
            }
            return ((LocalDate) o1.get("start")).compareTo((LocalDate) o2.get("start"));
        });

        for (int i = 1; i < sortedPositions.size(); i++) {
            final var previousPosition = sortedPositions.get(i - 1);
            final var position = sortedPositions.get(i);

            if (((LocalDate) position.get("start")).isBefore(((LocalDate) previousPosition.get("end")))) {
                failures.add(new ValidationFailure()
                        .fieldId("contributor[%d].position[%d].startDate".formatted(contributorIndex, (int)position.get("index")))
                        .errorType(INVALID_VALUE_TYPE)
                        .message("Contributors can only hold one position at any given time. This position conflicts with contributor[%d].position[%d]"
                                .formatted(contributorIndex, (int) previousPosition.get("index")))
                );

            }
        }

        return failures;
    }
}

