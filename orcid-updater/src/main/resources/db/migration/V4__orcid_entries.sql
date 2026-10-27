-- RAID-1004: the membership entries the updater writes to researchers' ORCID records.

-- One row per researcher and RAiD. The put code is ORCID's id for the entry, needed to update or
-- delete it. The last outcome is kept here so operators can see failed writes; there are no
-- background retries, so a failure stays until the next save of the RAiD tries again.
CREATE TABLE orcid_entry
(
    -- the iD as a URL, as on the RAiD
    orcid_id        text        NOT NULL,
    raid_handle     text        NOT NULL,
    -- null until ORCID has accepted the entry
    put_code        bigint,
    last_attempt_at timestamptz NOT NULL,
    last_success_at timestamptz,
    -- why the last attempt failed, for operators: the HTTP status and ORCID's message, never a token.
    -- Null when the last attempt succeeded.
    failure         text,
    PRIMARY KEY (orcid_id, raid_handle)
);

-- "Which entries does the updater hold for this RAiD?", to remove contributors who left it
CREATE INDEX orcid_entry_by_raid_handle ON orcid_entry (raid_handle);

-- Set when ORCID rejects the researcher's token (they revoked access). The updater stops writing
-- for the iD until the researcher authenticates again, which clears it.
ALTER TABLE researcher
    ADD COLUMN revoked_at timestamptz;
