package au.org.raid.api.validator;

import au.org.raid.api.util.DateUtil;
import au.org.raid.api.util.SchemaValues;
import au.org.raid.idl.raidv2.model.Title;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static au.org.raid.api.endpoint.message.ValidationMessage.*;
import static au.org.raid.api.util.StringUtil.isBlank;
import static java.util.Collections.emptyList;

@Service
@RequiredArgsConstructor
public class TitleValidator {
    private final TitleTypeValidator titleTypeValidationService;
    private final LanguageValidator languageValidator;

    public List<ValidationFailure> validatePrimaryTitle(
            List<Title> titles
    ) {

        var primaryTitles = getPrimaryTitles(titles);

        if (primaryTitles.size() == 0) {
            return List.of(AT_LEAST_ONE_PRIMARY_TITLE);
        }

        if (primaryTitles.size() > 1) {
            // check dates
            return validatePrimaryTitleDates(titles);
        }

        return emptyList();
    }

    public List<ValidationFailure> validate(List<Title> titles) {
        if (titles == null) {
            return List.of(TITLES_NOT_SET);
        }

        var failures = new ArrayList<ValidationFailure>();

        failures.addAll(validatePrimaryTitle(titles));

        IntStream.range(0, titles.size()).forEach(index -> {
            var title = titles.get(index);

            if (isBlank(title.getText())) {
                failures.add(titleNotSet(index));
            }
            if (title.getStartDate() == null) {
                failures.add(titleStartDateNotSet(index));
            }
            else if (!isBlank(title.getEndDate()) && DateUtil.parseDate(title.getEndDate()).isBefore(DateUtil.parseDate(title.getStartDate()))) {
                failures.add(new ValidationFailure()
                        .fieldId("title[%d].endDate". formatted(index))
                        .errorType(INVALID_VALUE_TYPE)
                        .message(END_DATE_BEFORE_START_DATE)
                );
            }

            failures.addAll(titleTypeValidationService.validate(title.getType(), index));
            failures.addAll(languageValidator.validate(title.getLanguage(), "title[%d]".formatted(index)));
        });
        return failures;
    }

    public List<Title> getPrimaryTitles(List<Title> titles) {
        return titles.stream().filter(title ->
                title.getType().getId() != null && title.getType().getId().equals(SchemaValues.PRIMARY_TITLE_TYPE.getUri())
        ).toList();
    }

    private List<ValidationFailure> validatePrimaryTitleDates(final List<Title> titles) {
        final var failures = new ArrayList<ValidationFailure>();
        final var today = LocalDate.now();

        var primaryTitles = titles.stream()
                .filter(title -> title.getType().getId().equals(SchemaValues.PRIMARY_TITLE_TYPE.getUri()))
                .sorted((o1, o2) -> {
                    if (o1.getStartDate().equals(o2.getStartDate())) {
                        final var o1EndDate = o1.getEndDate() == null ? LocalDate.now() : DateUtil.parseDate(o1.getEndDate());
                        final var o2EndDate = o2.getEndDate() == null ? LocalDate.now() : DateUtil.parseDate(o2.getEndDate());

                        return o1EndDate.compareTo(o2EndDate);
                    }
                    return DateUtil.parseDate(o1.getStartDate()).compareTo(DateUtil.parseDate(o2.getStartDate()));
                })
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 1; i < primaryTitles.size(); i++) {
            final var previous = primaryTitles.get(i - 1);
            final var title = primaryTitles.get(i);
            final var previousIndex = titles.indexOf(previous);
            final var index = titles.indexOf(title);

            final var startDate = DateUtil.parseDate(title.getStartDate());
            final var endDate = (previous.getEndDate() != null) ? DateUtil.parseDate(previous.getEndDate()) : today;

            if (title.equals(previous)) {
                return List.of(new ValidationFailure()
                        .fieldId("title[%d]".formatted(index))
                        .errorType(DUPLICATE_TYPE)
                        .message(DUPLICATE_MESSAGE)
                );
            } else if (startDate.isBefore(endDate)) {
                failures.add(new ValidationFailure()
                        .fieldId("title[%d].startDate".formatted(index))
                        .errorType(INVALID_VALUE_TYPE)
                        .message("There can only be one primary title in any given period. The start date for this title overlaps with title[%d]".formatted(previousIndex))
                );

            }
        }

        return failures;
    }
}