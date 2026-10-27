-- RAID-1005: status propagation and RAiD syncs, worked through by the updater in the background.

-- What still has to be done, so nothing is lost if the updater restarts. One row per thing to do:
-- a second request for the same RAiD or iD while the first is waiting is merged into it.
CREATE TABLE work_item
(
    -- STATUS: push an iD's current status to the RAiD API (and write its RAiDs if authenticated).
    -- RAID_SYNC: read a RAiD from the API and bring its ORCID entries in step with it.
    kind            text        NOT NULL,
    -- the iD as a URL for STATUS, the RAiD handle for RAID_SYNC
    item_key        text        NOT NULL,
    requested_at    timestamptz NOT NULL,
    -- goes up with every request, so finishing an item doesn't drop a request made meanwhile
    generation      integer     NOT NULL DEFAULT 0,
    -- when it may next be worked on; later than requested_at after a failed attempt
    next_attempt_at timestamptz NOT NULL,
    -- set while an updater instance is working on it, so two instances don't both take it
    claimed_until   timestamptz,
    attempts        integer     NOT NULL DEFAULT 0,
    -- set when the RAiD API stayed unreachable for every attempt. Kept for operators; not retried
    -- until something requests it again.
    failed_at       timestamptz,
    -- the last failure, for operators: never a token
    failure         text,
    PRIMARY KEY (kind, item_key),
    CONSTRAINT work_item_kind CHECK (kind IN ('STATUS', 'RAID_SYNC'))
);

CREATE INDEX work_item_due ON work_item (next_attempt_at) WHERE failed_at IS NULL;

-- The updater's key for signing its token requests to Keycloak (private_key_jwt), generated on
-- first start. Keycloak reads the public half from the updater's JWKS endpoint (RAID-1011), so no
-- client secret is ever created or copied. One row; every updater instance uses the same key.
CREATE TABLE service_key
(
    key_id      text PRIMARY KEY,
    -- PKCS#8 private key, encrypted with orcid-updater.token-encryption-key
    private_key bytea       NOT NULL,
    -- X.509 public key, published as a JWK
    public_key  bytea       NOT NULL,
    created_at  timestamptz NOT NULL
);

-- Set when the expiry of an unused invitation has been queued as a status change, so it is
-- queued once.
ALTER TABLE invitation
    ADD COLUMN expiry_queued_at timestamptz;

CREATE INDEX invitation_expired_unqueued ON invitation (expires_at) WHERE used_at IS NULL AND expiry_queued_at IS NULL;
