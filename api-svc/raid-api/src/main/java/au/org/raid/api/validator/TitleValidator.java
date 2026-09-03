package au.org.raid.api.validator;

import au.org.raid.api.util.DateUtil;
import au.org.raid.idl.raidv2.model.Title;
import au.org.raid.idl.raidv2.model.TitleTypeIdEnum;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            if (isBlank(title.getStartDate())) {
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
                title.getType().getId() != null && title.getType().getId() == TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_5
        ).toList();
    }

    private List<ValidationFailure> validatePrimaryTitleDates(final List<Title> titles) {
        final var failures = new ArrayList<ValidationFailure>();
        final var today = LocalDate.now();

        var primaryTitles = titles.stream()
                .filter(title -> title.getType().getId() == TitleTypeIdEnum.HTTPS_VOCABULARY_RAID_ORG_TITLE_TYPE_SCHEMA_5)
                .toList();

        // resolve each title's start/end into concrete LocalDates up front (blank -> today) so the
        // comparator and overlap check below never touch a raw, possibly-blank date String.
        var resolvedTitles = new ArrayList<Map<String, Object>>();
        for (final var title : primaryTitles) {
            resolvedTitles.add(Map.of(
                    "title", title,
                    "start", isBlank(title.getStartDate()) ? today : DateUtil.parseDate(title.getStartDate()),
                    "end", isBlank(title.getEndDate()) ? today : DateUtil.parseDate(title.getEndDate())
            ));
        }

        resolvedTitles.sort((o1, o2) -> {
            final var o1Start = (LocalDate) o1.get("start");
            final var o2Start = (LocalDate) o2.get("start");

            if (o1Start.equals(o2Start)) {
                return ((LocalDate) o1.get("end")).compareTo((LocalDate) o2.get("end"));
            }
            return o1Start.compareTo(o2Start);
        });

        for (int i = 1; i < resolvedTitles.size(); i++) {
            final var previousEntry = resolvedTitles.get(i - 1);
            final var titleEntry = resolvedTitles.get(i);
            final var previous = (Title) previousEntry.get("title");
            final var title = (Title) titleEntry.get("title");
            final var previousIndex = titles.indexOf(previous);
            final var index = titles.indexOf(title);

            final var startDate = (LocalDate) titleEntry.get("start");
            final var endDate = (LocalDate) previousEntry.get("end");

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