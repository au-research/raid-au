package au.org.raid.orcidupdater.orcid;

import lombok.Getter;

/**
 * ORCID refused or failed a membership call. The message is for operators: the HTTP status and
 * ORCID's developer message, never the researcher's token.
 */
@Getter
public class OrcidWriteException extends RuntimeException {
    public enum Kind {
        /** 401: the researcher revoked the updater's access, or the token is no longer valid. */
        TOKEN_REJECTED,
        /** 404: ORCID has no entry with this put code. */
        NOT_FOUND,
        /** 5xx, 429 or no response: worth trying again. */
        RETRYABLE,
        /** Any other refusal, e.g. 400 or 409. Trying again won't help. */
        REJECTED
    }

    private final Kind kind;

    public OrcidWriteException(final Kind kind, final String message) {
        super(message);
        this.kind = kind;
    }
}
