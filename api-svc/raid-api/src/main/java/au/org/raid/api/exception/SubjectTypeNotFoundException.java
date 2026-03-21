package au.org.raid.api.exception;

public class SubjectTypeNotFoundException extends RuntimeException {
    public SubjectTypeNotFoundException(final String id) {
        super("Subject type not found %s".formatted(id));
    }
}
