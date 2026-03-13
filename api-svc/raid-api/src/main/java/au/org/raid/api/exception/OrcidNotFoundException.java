package au.org.raid.api.exception;

public class OrcidNotFoundException extends RuntimeException {
    public OrcidNotFoundException(final String orcid) {
        super("ORCID not found: %s".formatted(orcid));
    }
}
