package au.org.raid.orcidupdater.signin;

/** The researcher's invitation link can't be used. Never carries the token. */
public class InvalidInvitationLinkException extends RuntimeException {
    public enum Reason { UNKNOWN, USED, EXPIRED }

    private final Reason reason;

    public InvalidInvitationLinkException(final Reason reason) {
        super("Invitation link " + reason.name().toLowerCase());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
