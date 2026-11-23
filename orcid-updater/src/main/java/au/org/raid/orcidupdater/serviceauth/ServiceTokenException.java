package au.org.raid.orcidupdater.serviceauth;

/** The updater couldn't get its own access token from Keycloak. */
public class ServiceTokenException extends RuntimeException {
    public ServiceTokenException(final String message) {
        super(message);
    }
}
