package au.org.raid.orcidupdater.status;

/**
 * A contributor's status, per person. The RAiD API's existing values, so Member API integrators
 * see no change (Decision on RAID-974, 2026-10-23). AUTHENTICATION_FAILED is not used.
 */
public enum ContributorStatus {
    AUTHENTICATED,
    AWAITING_AUTHENTICATION,
    AUTHENTICATION_REVOKED,
    UNAUTHENTICATED
}
