# RAID-1004: Write RAiDs to researchers' ORCID records

- JIRA: [RAID-1004](https://ardc.atlassian.net/browse/RAID-1004), sub-task of [RAID-975](https://ardc.atlassian.net/browse/RAID-975) (epic [RAID-974](https://ardc.atlassian.net/browse/RAID-974))
- Earlier sub-tasks: [RAID-1000](https://ardc.atlassian.net/browse/RAID-1000) (PR #741), [RAID-1001](https://ardc.atlassian.net/browse/RAID-1001) (PR #744), [RAID-1002](https://ardc.atlassian.net/browse/RAID-1002) (PR #745), [RAID-1003](https://ardc.atlassian.net/browse/RAID-1003) (PR #746)
- Next: [RAID-1005](https://ardc.atlassian.net/browse/RAID-1005) calls the writer when a RAiD or a contributor's status changes, and holds back embargoed RAiDs
- PR: to be opened (targets `feature/RAID-975`)
- Status: built and tested locally, not yet committed

## What changed and why

RAID-1003 stores a researcher's ORCID token when they authenticate. This
sub-task adds `OrcidWriter.sync(raid)`, which keeps a RAiD's entries on its
contributors' ORCID records in step with the RAiD:

- Writes one membership entry for each authenticated contributor, in ORCID
  JSON. The fields follow the central updater (raid-aws-private
  `handler/orcid-writer`), so RAiD AU's existing entries keep their shape:
  title, role from the latest position, that position's dates, the owner
  organisation, and the RAiD URL as a `raid` external id.
- Keeps ORCID's put code per researcher and RAiD (new `orcid_entry` table,
  migration V4), so later saves update the same entry. If ORCID no longer
  has the put code, the entry is created again.
- Deletes the entries of contributors removed from the RAiD.
- Retries 5xx, 429 and no response with back-off (3 attempts from 2 seconds,
  configurable). The last failure is kept on the `orcid_entry` row with
  ORCID's message, never a token.
- A 401 from ORCID marks the researcher revoked (new `researcher.revoked_at`).
  Nothing more is written for them, and their email fingerprint stops
  matching invitations, until they authenticate again.
- Each researcher's write holds that iD's advisory lock.

Nothing calls `sync` yet; RAID-1005 does.

### Which organisation an entry names

The RAiD schema has no affiliation for a contributor. Rob decided first to
name the owner organisation only on a principal investigator's entry. The
ORCID sandbox then rejected a membership without an organisation
(`400 Bad request: organization must be present and valid`), although
ORCID's affiliations tutorial says it is optional when an external id is
given. Rob decided on 2026-10-27 that every entry names the organisation
that owns the RAiD (`identifier.owner.id`), looked up in ROR, as the central
updater does. If ROR can't find it, the write is recorded as failed without
calling ORCID. The ADR records the decision and when to revisit it.

### Settings

| Environment variable | Default |
|---|---|
| `ORCID_UPDATER_ORCID_WRITE_MAX_ATTEMPTS` | `3` |
| `ORCID_UPDATER_ORCID_WRITE_INITIAL_BACKOFF` | `2s` |
| `ORCID_UPDATER_ROR_API_URL` | `https://api.ror.org/v2` |

## Testing

- Unit tests: 163 pass, including MembershipFactory, OrcidRaid, RorClient,
  OrcidMemberClient and OrcidWriter.
- Integration tests: 30 pass against PostgreSQL in Testcontainers, including
  6 for the writer (create then update, recreate, organisation, revocation,
  retries, removal) with ORCID and ROR stubbed.
- Build with `--release 17` passes.
- ORCID sandbox, 2026-10-27, with the demo ORCID client and Rob's sandbox
  iD: `OrcidSandboxIntTest` wrote a principal investigator entry and a
  co-investigator entry with the updater's real classes and ROR, updated one
  by put code, and read both back. ORCID stored every field as sent, and
  Rob checked both on the sandbox record. The test entries were deleted
  afterwards. The test is skipped unless `ORCID_SANDBOX_TOKEN_FILE` is set.
- Entries written by the API took the record's default visibility
  (`private` on Rob's sandbox account).

## Documentation

- `orcid-updater/README.md`: new settings and what the updater writes.
- `doc/adr/2026-10-23_per-ra-orcid-updater.md`: new section "What each
  ORCID entry says".
