package au.org.raid.api.exception;

public class SubjectTypeNotFoundException extends RuntimeException {
    public SubjectTypeNotFoundException(final Integer id) {
        super("Subject type not found %d".formatted(id));
    }

    public SubjectTypeNotFoundException(final String id) {
        super("Subject type not found %s".formatted(id));
    }
}
