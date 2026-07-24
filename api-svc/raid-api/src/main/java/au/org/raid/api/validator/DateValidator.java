package au.org.raid.api.validator;

import au.org.raid.api.endpoint.message.ValidationMessage;
import au.org.raid.api.util.DateUtil;
import au.org.raid.idl.raidv2.model.Date;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static au.org.raid.api.endpoint.message.ValidationMessage.END_DATE_BEFORE_START_DATE;
import static au.org.raid.api.endpoint.message.ValidationMessage.INVALID_VALUE_TYPE;
import static au.org.raid.api.util.StringUtil.isBlank;

@Component
public class DateValidator {
    public List<ValidationFailure> validate(final Date date) {
        final var failures = new ArrayList<ValidationFailure>();
        if (date == null) {
            failures.add(ValidationMessage.DATES_NOT_SET);
        } else {
            if (isBlank(date.getStartDate())) {
                failures.add(ValidationMessage.DATES_START_DATE_NOT_SET);
            } else if (!isBlank(date.getEndDate()) && DateUtil.parseDate(date.getEndDate()).isBefore(DateUtil.parseDate(date.getStartDate()))) {
                failures.add(new ValidationFailure()
                        .fieldId("date.endDate")
                        .errorType(INVALID_VALUE_TYPE)
                        .message(END_DATE_BEFORE_START_DATE)
                );
            }
        }
        return failures;
    }
}
