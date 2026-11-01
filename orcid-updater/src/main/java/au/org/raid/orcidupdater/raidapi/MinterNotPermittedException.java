package au.org.raid.orcidupdater.raidapi;

public class MinterNotPermittedException extends RuntimeException {
    public MinterNotPermittedException(final String handle) {
        super("Minter may not edit %s".formatted(handle));
    }
}
