package au.org.raid.orcidupdater.invitation;

public class ContributorNotOnRaidException extends RuntimeException {
    public ContributorNotOnRaidException(final String contributorId, final String handle) {
        super("%s is not a contributor on %s".formatted(contributorId, handle));
    }
}
