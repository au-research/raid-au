# RAID-1003: Researcher pages and ORCID sign-in for the ORCID updater

- JIRA: [RAID-1003](https://ardc.atlassian.net/browse/RAID-1003), sub-task of [RAID-975](https://ardc.atlassian.net/browse/RAID-975) (epic [RAID-974](https://ardc.atlassian.net/browse/RAID-974))
- Earlier sub-tasks: [RAID-1000](https://ardc.atlassian.net/browse/RAID-1000) (PR #741), [RAID-1001](https://ardc.atlassian.net/browse/RAID-1001) (PR #744), [RAID-1002](https://ardc.atlassian.net/browse/RAID-1002) (PR #745)
- Related: [RAID-1010](https://ardc.atlassian.net/browse/RAID-1010) (CI skips the updater's integration tests)
- PR: [#746](https://github.com/au-research/raid-au/pull/746) (targets `feature/RAID-975`)
- Status: PR open

## What changed and why

RAID-1002 emails researchers an invitation link. This sub-task adds what
happens when they open it: the updater serves the landing page, sends the
researcher to ORCID, and records the result on ORCID's callback.

### Pages

Server-rendered Thymeleaf templates with no JavaScript:

- landing page: what a RAiD is, the RAiD title, service point and ORCID client
  name, the permissions requested, an unticked "Remember me for future RAiDs
  at this RA" box and the link expiry date
- confirmation page: what will be written, how to revoke access at ORCID, and
  what "Remember me" stored
- pages for a declined sign-in, an invalid link (404 unknown, 410 used or
  expired), an invalid sign-in state (400) and an ORCID failure (502)

Each page names the RA's contact address (new setting
`ORCID_UPDATER_CONTACT_EMAIL`) for questions and deletion requests.

### Sign-in

- `POST /invitations/{token}` creates a single-use OAuth state (stored as a
  SHA-256 hash, expires after 30 minutes) and redirects (303) to ORCID with
  the scopes `/read-limited /activities/update`.
- `GET /orcid/callback` consumes the state, exchanges the code, marks the
  invitation used, and stores the access and refresh tokens encrypted with
  AES-GCM (`TokenCipher`, the iD as associated data).
- With "Remember me" ticked, the email fingerprint carried in the link is
  decrypted and stored against the iD. The latest consent wins.
- Declining at ORCID leaves the link usable.
- If the researcher signs in with a different iD from the one the minter
  typed, the authenticated iD is recorded and shown on the confirmation page.
- Migration `V3__orcid_sign_in.sql` adds the `oauth_state` table and the token
  and invitation columns.

### Protections

- `Referrer-Policy: no-referrer`, so the invitation token in the page URL never
  reaches ORCID in a Referer header.
- A strict Content Security Policy: no scripts, and forms may only post to the
  updater or the configured ORCID host.
- `Cache-Control: no-store` on every page.
- Concurrent callbacks are safe: state consumption uses `DELETE ... RETURNING`,
  the invitation is marked used only if not already used, and writes per iD
  hold a PostgreSQL advisory lock.
- ORCID error responses and tokens are never logged.

### Decisions recorded

The ADR (`doc/adr/2026-10-23_per-ra-orcid-updater.md`) now records the scope
choice with its source (ORCID's affiliations tutorial), the token revoke API,
and that the exact ORCID revoke button wording is unverified because ORCID's
help centre refuses automated requests.

## Testing

- Unit tests: 152 pass, including new `TokenCipherTest`, `OrcidOAuthClientTest`,
  `SignInServiceTest` and `ResearcherPagesControllerTest`, and updated
  `SecurityConfigTest` checks for the headers and policy.
- Integration tests: 24 pass locally, including the new `SignInFlowIntTest`
  against PostgreSQL. CI doesn't run them yet (RAID-1010).
- Compiles with `--release 17`.
- Local run in the dev profile: landing page 200 with the expected headers,
  POST 303 to sandbox ORCID, unknown link 404, forged or missing state 400.

## Not in this sub-task

- Writing RAiDs to ORCID (RAID-1004), including Matthias's sandbox check that a
  membership needs no organisation when an external ID is given.
- Pushing the status to the RAiD API (RAID-1005).
- Deletion and token revocation (RAID-1006).
