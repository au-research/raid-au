package au.org.raid.api.validator;

import au.org.raid.api.client.contributor.ContributorClient;
import au.org.raid.api.config.properties.ContributorValidationProperties.ContributorTypeValidationProperties;
import au.org.raid.api.util.DateUtil;
import au.org.raid.idl.raidv2.model.Contributor;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ValidationFailure;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.endpoint.message.ValidationMessage.INVALID_VALUE_MESSAGE;
import static au.org.raid.api.endpoint.message.ValidationMessage.INVALID_VALUE_TYPE;
import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_MESSAGE;
import static au.org.raid.api.endpoint.message.ValidationMessage.NOT_SET_TYPE;
import static au.org.raid.api.util.StringUtil.isBlank;

public class ContributorTypeValidator {
    private final ContributorTypeValidationProperties validationProperties;
    private final ContributorClient contributorClient;
    private final ContributorRoleValidator roleValidator;
    private final ContributorPositionValidator positionValidator;

    public ContributorTypeValidator(
            ContributorTypeValidationProperties validationProperties,
            ContributorClient contributorClient,
            ContributorRoleValidator roleValidator,
            ContributorPositionValidator positionValidator
    ) {
        this.validationProperties = validationProperties;
        this.contributorClient = contributorClient;
        this.roleValidator = roleValidator;
        this.positionValidator = positionValidator;
    }

    public List<ValidationFailure> validate(final Contributor contributor, final int index) {
        final var failures = new ArrayList<ValidationFailure>();

        if (isBlank(contributor.getId())) {
            failures.add(
                    new ValidationFailure()
                            .fieldId("contributor[%d].id".formatted(index))
                            .errorType(NOT_SET_TYPE)
                            .message(NOT_SET_MESSAGE)
            );
        }
        else if (contributor.getId().startsWith(validationProperties.getUrlPrefix())) {
            if (!contributorClient.exists(contributor.getId())) {
                failures.add(
                        new ValidationFailure()
                                .fieldId("contributor[%d].id".formatted(index))
                                .errorType(NOT_FOUND_TYPE)
                                .message("This id does not exist")
                );
            }
        } else {
            failures.add(
                    new ValidationFailure()
                            .fieldId("contributor[%d].id".formatted(index))
                            .errorType(INVALID_VALUE_TYPE)
                            .message("Contributor id should start with " + validationProperties.getUrlPrefix())
            );
        }

        if (isBlank(contributor.getSchemaUri())) {
            failures.add(
                    new ValidationFailure()
                            .fieldId("contributor[%d].schemaUri".formatted(index))
                            .errorType(NOT_SET_TYPE)
                            .message(NOT_SET_MESSAGE)
            );
        }
        else if (!contributor.getSchemaUri().matches(validationProperties.getSchemaUri())) {
            failures.add(new ValidationFailure()
                    .fieldId("contributor[%d].schemaUri".formatted(index))
                    .errorType(INVALID_VALUE_TYPE)
                    .message(INVALID_VALUE_MESSAGE + " - should be %s".formatted(validationProperties.getSchemaUri()))
            );
        }

        IntStream.range(0, contributor.getRole().size())
                .forEach(roleIndex -> {
                    final var role = contributor.getRole().get(roleIndex);
                    failures.addAll(roleValidator.validate(role, index, roleIndex));
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
                        failures.addAll(positionValidator.validate(position, index, positionIndex));
                    });

            failures.addAll(validatePositions(contributor.getPosition(), index));
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
