package au.org.raid.api.dto;

public enum ContributorStatus {
    AUTHENTICATED,
    UNAUTHENTICATED,
    AWAITING_AUTHENTICATION,
    AUTHENTICATION_FAILED,
    AUTHENTICATION_REVOKED
}
