package au.org.raid.orcidupdater.signin;

/** ORCID sign-in could not be completed on ORCID's side. The invitation link still works. */
public class OrcidSignInFailedException extends RuntimeException {
    public OrcidSignInFailedException(final String message) {
        super(message);
    }
}
