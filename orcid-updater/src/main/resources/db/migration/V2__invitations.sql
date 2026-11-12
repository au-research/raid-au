-- RAID-1002: invitations and the consented email fingerprint.
--
-- Neither table holds an email address. The email fingerprint is not stored for a pending
-- invitation either: it travels encrypted in the invitation link and is written to researcher
-- only when the researcher ticks "Remember me" (Decision on RAID-1002, 2026-10-26).

CREATE TABLE invitation
(
    id          uuid PRIMARY KEY,
    -- SHA-256 of the random part of the link token; the token itself is never stored
    token_hash  bytea       NOT NULL UNIQUE,
    -- the ORCID iD the minter typed, as a URL
    orcid_id    text        NOT NULL,
    raid_handle text        NOT NULL,
    created_at  timestamptz NOT NULL,
    expires_at  timestamptz NOT NULL,
    used_at     timestamptz
);

-- "Is an invitation already pending for this person?" (one invitation per researcher at a time)
CREATE INDEX invitation_pending_by_orcid_id ON invitation (orcid_id, expires_at) WHERE used_at IS NULL;

-- One row per person. RAID-1003 adds the authenticated token columns.
CREATE TABLE researcher
(
    -- the iD ORCID returned at sign-in, as a URL
    orcid_id     text PRIMARY KEY,
    -- HMAC-SHA-256 of the normalised email, present only with consent
    email_hash   bytea UNIQUE,
    consented_at timestamptz,
    CONSTRAINT researcher_consent_recorded CHECK ((email_hash IS NULL) = (consented_at IS NULL))
);
