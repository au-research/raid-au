package au.org.raid.orcidupdater.raidapi;

public class RaidNotFoundException extends RuntimeException {
    public RaidNotFoundException(final String handle) {
        super("No RAiD with handle %s".formatted(handle));
    }
}
