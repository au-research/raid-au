package au.org.raid.api.config.servicepoint;

/**
 * Thrown during startup when this instance's Registration Agency cannot be
 * resolved to a Service Point ID block. Deliberately fatal: an instance with no
 * allocated block would mint Service Point IDs that collide with another
 * agency's in federated RAiD metadata.
 */
public class RegistrationAgencyNotRegisteredException extends RuntimeException {
    public RegistrationAgencyNotRegisteredException(final String message) {
        super(message);
    }
}
