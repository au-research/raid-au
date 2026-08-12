package au.org.raid.api.validator;

import au.org.raid.idl.raidv2.model.UnavailableResolver;
import au.org.raid.idl.raidv2.model.ValidationFailure;

import java.util.ArrayList;
import java.util.List;

/**
 * The combined outcome of a top-level validator (RAID-809): the ordinary 400 validation
 * failures it found, alongside any external resolvers that could not be reached while
 * validating individual items. Carrying both - rather than throwing away one in favour of
 * the other - lets {@link ValidationService} apply cause-based precedence (400 wins whenever
 * there is at least one resolvable failure; 503 only when unavailability is the sole blocker).
 */
public record ValidationResult(List<ValidationFailure> failures, List<UnavailableResolver> unavailableResolvers) {

    public static ValidationResult of(final List<ValidationFailure> failures) {
        return new ValidationResult(failures, List.of());
    }

    /**
     * Merges this result with another, concatenating both failures and unavailableResolvers.
     */
    public ValidationResult merge(final ValidationResult other) {
        final var mergedFailures = new ArrayList<>(this.failures);
        mergedFailures.addAll(other.failures());

        final var mergedUnavailable = new ArrayList<>(this.unavailableResolvers);
        mergedUnavailable.addAll(other.unavailableResolvers());

        return new ValidationResult(mergedFailures, mergedUnavailable);
    }
}
