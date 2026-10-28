package au.org.raid.orcidupdater.deletion;

/** The email given belongs to a different researcher from the iD given. Nothing is deleted. */
public class ResearcherMismatchException extends RuntimeException {
    public ResearcherMismatchException(final String orcidId, final String emailOwner) {
        super("The email belongs to %s, not %s".formatted(emailOwner, orcidId));
    }
}
