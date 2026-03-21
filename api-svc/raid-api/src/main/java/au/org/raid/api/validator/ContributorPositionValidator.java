package au.org.raid.api.validator;

import au.org.raid.api.repository.ContributorPositionRepository;
import au.org.raid.api.repository.ContributorPositionSchemaRepository;
import au.org.raid.api.util.DateUtil;
import au.org.raid.idl.raidv2.model.ContributorPosition;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
public class ContributorPositionValidator {
    private final ContributorPositionSchemaRepository contributorPositionSchemaRepository;
    private final ContributorPositionRepository contributorPositionRepository;

    public ContributorPositionValidator(final ContributorPositionSchemaRepository contributorPositionSchemaRepository, final ContributorPositionRepository contributorPositionRepository) {
        this.contributorPositionSchemaRepository = contributorPositionSchemaRepository;
        this.contributorPositionRepository = contributorPositionRepository;
    }

    public List<ValidationFailure> validate(
            final ContributorPosition position, final int contributorIndex, final int positionIndex) {
        final var failures = new ArrayList<ValidationFailure>();

        if (position.getStartDate() == null) {
            failures.add(
                    new ValidationFailure()
                            .fieldId("contributor[%d].position[%d].startDate".formatted(contributorIndex, positionIndex))
                            .errorType(NOT_SET_TYPE)
                            .message(NOT_SET_MESSAGE));
        } else if (!isBlank(position.getEndDate()) && DateUtil.parseDate(position.getEndDate()).isBefore(DateUtil.parseDate(position.getStartDate()))) {
            failures.add(
                    new ValidationFailure()
                            .fieldId("contributor[%d].position[%d].endDate".formatted(contributorIndex, positionIndex))
                            .errorType(INVALID_VALUE_TYPE)
                            .message(END_DATE_BEFORE_START_DATE));
        }

        if (isBlank(position.getId())) {
            failures.add(
                    new ValidationFailure()
                            .fieldId("contributor[%d].position[%d].id".formatted(contributorIndex, positionIndex))
                            .errorType(NOT_SET_TYPE)
                            .message(NOT_SET_MESSAGE));
        }

        if (isBlank(position.getSchemaUri())) {
            failures.add(
                    new ValidationFailure()
                            .fieldId("contributor[%d].position[%d].schemaUri".formatted(contributorIndex, positionIndex))
                            .errorType(NOT_SET_TYPE)
                            .message(NOT_SET_MESSAGE)
            );
        } else {
            final var positionScheme =
                    contributorPositionSchemaRepository.findActiveByUri(position.getSchemaUri());

            if (positionScheme.isEmpty()) {
                failures.add(
                        new ValidationFailure()
                                .fieldId("contributor[%d].position[%d].schemaUri".formatted(contributorIndex, positionIndex))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_SCHEMA)
                );
            } else if (!isBlank(position.getId()) &&
                    contributorPositionRepository.findByUriAndSchemaId(position.getId(), positionScheme.get().getId()).isEmpty()) {
                failures.add(
                        new ValidationFailure()
                                .fieldId("contributor[%d].position[%d].id".formatted(contributorIndex, positionIndex))
                                .errorType(INVALID_VALUE_TYPE)
                                .message(INVALID_ID_FOR_SCHEMA)
                );
            }
        }

        return failures;
    }
}