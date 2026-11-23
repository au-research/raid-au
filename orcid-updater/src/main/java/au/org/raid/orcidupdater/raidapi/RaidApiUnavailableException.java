package au.org.raid.orcidupdater.raidapi;

public class RaidApiUnavailableException extends RuntimeException {
    public RaidApiUnavailableException(final String message) {
        super(message);
    }

    public RaidApiUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
