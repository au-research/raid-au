package au.org.raid.api.exception;

import au.org.raid.idl.raidv2.model.BudgetFailure;
import au.org.raid.idl.raidv2.model.ValidationFailure;
import lombok.Getter;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.List.copyOf;

@Getter
public class BudgetException extends RaidApiException {
    private static final String TITLE = "Budgeting errors.";
    private static final int STATUS = 402;
    private final List<BudgetFailure> failures;

    public BudgetException(Collection<BudgetFailure> failures) {
        super();
        this.failures = copyOf(failures);
    }

    public String getTitle() {
        return TITLE;
    }

    public int getStatus() {
        return STATUS;
    }

    public String getDetail() {
        return String.format(
                "Request had %d budget error(s). See failures for more details...",
                failures.size());
    }
    @Override
    public String getMessage() {
        return getFailures().stream().
                map(i -> "%s - %s".formatted(i.getFieldId(), i.getMessage())).
                collect(Collectors.joining(","));
    }
}
