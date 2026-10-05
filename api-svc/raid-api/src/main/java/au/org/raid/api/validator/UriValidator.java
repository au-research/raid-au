package au.org.raid.api.validator;

import au.org.raid.idl.raidv2.model.ValidationFailure;

import java.util.List;

public interface UriValidator {
    /**
     * Full validation: the local checks of {@link #validateLocally} followed by the check
     * against the external resolver.
     */
    List<ValidationFailure> validate(String uri, String fieldId);

    /**
     * The checks that need no network call (format, and any scheme-specific plausibility
     * checks). Used on its own for a uri already confirmed against the resolver (RAID-935).
     * {@link #validate} must run these first and only contact the resolver when this returns
     * no failures.
     */
    List<ValidationFailure> validateLocally(String uri, String fieldId);
}
