package au.org.raid.orcidupdater.signin;

/** The OAuth callback matches no sign-in in progress (unknown, used or expired state, or no code). */
public class InvalidOAuthStateException extends RuntimeException {
    public InvalidOAuthStateException(final String message) {
        super(message);
    }
}
