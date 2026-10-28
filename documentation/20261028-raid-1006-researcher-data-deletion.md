# RAID-1006: Deleting a researcher's data from the ORCID updater

- JIRA: [RAID-1006](https://ardc.atlassian.net/browse/RAID-1006), sub-task of [RAID-975](https://ardc.atlassian.net/browse/RAID-975) (epic [RAID-974](https://ardc.atlassian.net/browse/RAID-974))
- Earlier sub-task: [RAID-1011](https://ardc.atlassian.net/browse/RAID-1011) (PR [#749](https://github.com/au-research/raid-au/pull/749))
- PR: [#750](https://github.com/au-research/raid-au/pull/750) (targets `feature/RAID-975`)
- Status: PR open

## What changed and why

The decision on RAID-975 (Q5, 2026-10-23) gives RA administrators an
endpoint to act on a researcher's request to delete their stored data.

- `POST /admin/researcher-deletions` takes the researcher's email, ORCID iD
  or both. It needs the realm role `orcid-updater-admin`, separate from the
  RAiD API's credential.
- In one transaction, holding the lock on the iD, the updater:
  1. deletes each entry it wrote to the researcher's ORCID record (an entry
     already gone at ORCID counts as deleted);
  2. revokes its token at ORCID (`POST /oauth/revoke`);
  3. deletes the `orcid_entry` rows, the invitations sent to or used to sign
     in as the iD (and their sign-ins in progress), and the researcher row
     with its keyed hash, consent time and token;
  4. queues a status update so the RAiD API shows the researcher as
     UNAUTHENTICATED.
- If ORCID fails, or refuses the revoke, the transaction rolls back and the
  response is 502 `ORCID_UNAVAILABLE`. Nothing is deleted, so the request
  can be sent again.
- If ORCID no longer accepts the token (the researcher revoked access
  there), the entries can't be removed. The data held here is still
  deleted, and `orcidEntriesLeft` in the response says how many entries the
  researcher can remove themselves.
- An email that belongs to a different researcher from the iD gives 400
  `RESEARCHER_MISMATCH` and deletes nothing.
- An email with no researcher behind it (they didn't tick "Remember me")
  still deletes any sign-ins in progress carrying its keyed hash.
- The email is never logged; the request's `toString` redacts it.

### ORCID's revoke mechanism

Checked in ORCID's source (`orcid-api-web/tutorial/revoke.md` and
`RevokeController.java` in ORCID-Source) and ORCID's FAQ:
`POST https://orcid.org/oauth/revoke` (sandbox: `sandbox.orcid.org`), form
fields `client_id`, `client_secret` and `token`, returns 200. Revoking the
access token or the refresh token revokes both; the researcher's other
tokens are unaffected. The status ORCID returns for a token that is already
invalid has not been checked against the sandbox.

## Testing

- Unit tests: 236 pass. New: `ResearcherDeletionServiceTest` (14),
  `ResearcherDeletionControllerTest` (6), and revoke tests in
  `OrcidOAuthClientTest`.
- Integration tests (Postgres): 51 run, 1 skipped (the manual ORCID sandbox
  test), 0 failures. `ResearcherDeletionIntTest` (5) covers deletion by
  email, ORCID failing then a retry, a researcher who revoked access
  earlier, a mismatch, and nothing held.
- Built with `--release 17`.
- The contract validates with `openApiValidate`.

## Documentation

- `orcid-updater/idl/orcid-updater-openapi-3.1.yaml`: the order of
  deletion, the 400 and 502 responses, and `orcidEntriesLeft`.
- `orcid-updater/README.md`: a section on deleting a researcher's data.
- `doc/adr/2026-10-23_per-ra-orcid-updater.md`: the revoke mechanism and
  the retry and entries-left behaviour.
