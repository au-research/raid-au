-- RAID-1003: the researcher pages and ORCID sign-in.

-- The landing page shows the RAiD title and service point name. They are copied from the RAiD
-- when the invitation is registered, because the updater can't read RAiDs on its own until it
-- has a RAiD API credential (RAID-1005). Existing rows predate this and have neither.
ALTER TABLE invitation
    ADD COLUMN raid_title            text,
    ADD COLUMN service_point_name    text,
    -- the iD ORCID returned at sign-in, which replaces the typed orcid_id (RAID-975)
    ADD COLUMN authenticated_orcid_id text;

-- One row per "Authenticate with ORCID" click. Used once, by the OAuth callback.
CREATE TABLE oauth_state
(
    -- SHA-256 of the state value sent to ORCID; the value itself is never stored
    state_hash    bytea PRIMARY KEY,
    invitation_id uuid        NOT NULL REFERENCES invitation (id) ON DELETE CASCADE,
    -- the email fingerprint, only when the researcher ticked "Remember me" on the landing page.
    -- It moves to researcher once ORCID sign-in succeeds, and is deleted with this row otherwise.
    email_hash    bytea,
    created_at    timestamptz NOT NULL,
    expires_at    timestamptz NOT NULL
);

-- Tokens are encrypted with orcid-updater.token-encryption-key, the iD as associated data
ALTER TABLE researcher
    ADD COLUMN access_token     bytea,
    ADD COLUMN refresh_token    bytea,
    ADD COLUMN scope            text,
    ADD COLUMN authenticated_at timestamptz;
