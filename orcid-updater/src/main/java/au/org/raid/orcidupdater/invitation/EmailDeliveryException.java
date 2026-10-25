package au.org.raid.orcidupdater.invitation;

/** The invitation email could not be sent. The message never contains the address. */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
