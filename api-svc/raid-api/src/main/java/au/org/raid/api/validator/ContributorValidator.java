package au.org.raid.api.validator;

import au.org.raid.api.dto.ContributorStatus;
import au.org.raid.api.repository.ContributorRepository;
import au.org.raid.api.util.DateUtil;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
@RequiredArgsConstructor
public class ContributorValidator {
    private static final String ORCID_ORG = "https://orcid.org/";
    private final ContributorPositionValidator positionValidationService;
    private final ContributorRoleValidator roleValidationService;
    private final ContributorRepository contributorRepository;

    public List<ValidationFailure> validate(
            List<Contributor> contributors
    ) {
        if (contributors == null || contributors.isEmpty()) {
            return List.of(CONTRIB_NOT_SET);
        }

        var failures = new ArrayList<ValidationFailure>();

        IntStream.range(0, contributors.size())
                .forEach(index -> {
                    final var contributor = contributors.get(index);

                    if (isBlank(contributor.getEmail())) {
                        // uuid must be present
//                        if (!isBlank(contributor.getId()) && !isBlank(contributor.getUuid())) {
//                            final var contributorOptional = contributorRepository.findByPidAndUuid(
//                                    contributor.getId(), contributor.getUuid()
//                            );
//
//                            if (contributorOptional.isEmpty()) {
//                                failures.add(
//                                        new ValidationFailure()
//                                                .fieldId("contributor[%d]".formatted(index))
//                                                .errorType(NOT_FOUND_TYPE)
//                                                .message("Contributor not found with PID (%s) and UUID (%s)"
//                                                        .formatted(contributor.getId(), contributor.getUuid())));
//
//                            }
//                        } else
                        if (!isBlank(contributor.getUuid())) {
                            final var contributorOptional = contributorRepository.findByUuid(
                                    contributor.getUuid()
                            );

                            if (contributorOptional.isEmpty()) {
                                failures.add(
                                        new ValidationFailure()
                                                .fieldId("contributor[%d].uuid".formatted(index))
                                                .errorType(NOT_FOUND_TYPE)
                                                .message("Contributor not found with UUID (%s)"
                                                        .formatted(contributor.getUuid())));

                            }
                        } else {
                            failures.add(
                                    new ValidationFailure()
                                            .fieldId("contributor[%d]".formatted(index))
                                            .errorType(NOT_SET_TYPE)
                                            .message("email or uuid is required"));
                        }
                    } else {
                        if (contributor.getUuid() != null) {
                            failures.add(
                                    new ValidationFailure()
                                            .fieldId("contributor[%d]".formatted(index))
                                            .errorType(INVALID_VALUE_TYPE)
                                            .message("email and uuid cannot be present at the same time"));
                        }

                        if (contributor.getId() != null) {
                            failures.add(
                                    new ValidationFailure()
                                            .fieldId("contributor[%d]".formatted(index))
                                            .errorType(INVALID_VALUE_TYPE)
                                            .message("email and id cannot be present at the same time"));
                        }

                    }

                    if (isBlank(contributor.getSchemaUri())) {
                        failures.add(
                                new ValidationFailure()
                                        .fieldId("contributor[%d].schemaUri".formatted(index))
                                        .errorType(NOT_SET_TYPE)
                                        .message(NOT_SET_MESSAGE)
                        );
                    } else if (!contributor.getSchemaUri().equals(ORCID_ORG)) {
                        failures.add(new ValidationFailure()
                                .fieldId("contributor[%d].schemaUri".formatted(index))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_VALUE_MESSAGE + " - should be " + ORCID_ORG)
                        );
                    }

                    IntStream.range(0, contributor.getRole().size())
                            .forEach(roleIndex -> {
                                final var role = contributor.getRole().get(roleIndex);
                                failures.addAll(roleValidationService.validate(role, index, roleIndex));
                            });

                    if (contributor.getPosition() == null || contributor.getPosition().isEmpty()) {
                        failures.add(new ValidationFailure()
                                .fieldId("contributor[%d]".formatted(index))
                                .errorType(NOT_SET_TYPE)
                                .message("A contributor must have a position")
                        );
                    } else {
                        IntStream.range(0, contributor.getPosition().size())
                                .forEach(positionIndex -> {
                                    final var position = contributor.getPosition().get(positionIndex);
                                    failures.addAll(positionValidationService.validate(position, index, positionIndex));
                                });

                        failures.addAll(validatePositions(contributor.getPosition(), index));
                    }
                });

        failures.addAll(validateLeader(contributors));
        failures.addAll(validateContact(contributors));

        return failures;
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

                        // uuid must be present
                    if (!isBlank(contributor.getUuid())) {
                        final var contributorOptional = contributorRepository.findByUuid(
                                contributor.getUuid()
                        );

                        if (contributorOptional.isEmpty()) {
                            failures.add(
                                    new ValidationFailure()
                                            .fieldId("contributor[%d].uuid".formatted(index))
                                            .errorType(NOT_FOUND_TYPE)
                                            .message("Contributor not found with UUID (%s)"
                                                    .formatted(contributor.getUuid())));

                        }
                    } else {
                        failures.add(
                                new ValidationFailure()
                                        .fieldId("contributor[%d].uuid".formatted(index))
                                        .errorType(NOT_SET_TYPE)
                                        .message("uuid is required"));
                    }

                    if (contributor.getId() == null) {
                        failures.add(
                            new ValidationFailure()
                                    .fieldId("contributor[%d].id".formatted(index))
                                    .errorType(NOT_SET_TYPE)
                                    .message("id is required"));
                    }

                    if (isBlank(contributor.getSchemaUri())) {
                        failures.add(
                                new ValidationFailure()
                                        .fieldId("contributor[%d].schemaUri".formatted(index))
                                        .errorType(NOT_SET_TYPE)
                                        .message(NOT_SET_MESSAGE)
                        );
                    } else if (!contributor.getSchemaUri().equals(ORCID_ORG)) {
                        failures.add(new ValidationFailure()
                                .fieldId("contributor[%d].schemaUri".formatted(index))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_VALUE_MESSAGE + " - should be " + ORCID_ORG)
                        );
                    }

                    IntStream.range(0, contributor.getRole().size())
                            .forEach(roleIndex -> {
                                final var role = contributor.getRole().get(roleIndex);
                                failures.addAll(roleValidationService.validate(role, index, roleIndex));
                            });

                    if (contributor.getPosition() == null || contributor.getPosition().isEmpty()) {
                        failures.add(new ValidationFailure()
                                .fieldId("contributor[%d]".formatted(index))
                                .errorType(NOT_SET_TYPE)
                                .message("A contributor must have a position")
                        );
                    } else {
                        IntStream.range(0, contributor.getPosition().size())
                                .forEach(positionIndex -> {
                                    final var position = contributor.getPosition().get(positionIndex);
                                    failures.addAll(positionValidationService.validate(position, index, positionIndex));
                                });

                        failures.addAll(validatePositions(contributor.getPosition(), index));
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

