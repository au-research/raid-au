package au.org.raid.orcidupdater.deletion;

/**
 * ORCID failed while removing the researcher's entries or revoking the token. The deletion is
 * rolled back so the administrator can try again; the message is for operators.
 */
public class OrcidUnavailableException extends RuntimeException {
    public OrcidUnavailableException(final String message) {
        super(message);
    }
}
