# RAID-1003: Researcher sign-in endpoints for the ORCID updater

- JIRA: [RAID-1003](https://ardc.atlassian.net/browse/RAID-1003), sub-task of [RAID-975](https://ardc.atlassian.net/browse/RAID-975) (epic [RAID-974](https://ardc.atlassian.net/browse/RAID-974))
- Earlier sub-tasks: [RAID-1000](https://ardc.atlassian.net/browse/RAID-1000) (PR #741), [RAID-1001](https://ardc.atlassian.net/browse/RAID-1001) (PR #744), [RAID-1002](https://ardc.atlassian.net/browse/RAID-1002) (PR #745)
- Related: [RAID-977](https://ardc.atlassian.net/browse/RAID-977) (the app's researcher pages), [RAID-1010](https://ardc.atlassian.net/browse/RAID-1010) (CI skips the updater's integration tests)
- PR: [#746](https://github.com/au-research/raid-au/pull/746) (targets `feature/RAID-975`)
- Status: PR open

## What changed and why

RAID-1002 emails researchers an invitation link. This sub-task adds the API
the RAiD app calls when the researcher opens it, signs in to ORCID and comes
back.

The first version of the PR had the updater serve its own HTML pages. Rob
rejected that on 2026-10-26: the researcher interface is built in React by
Abdul under RAID-977, and the updater provides API endpoints only. The ADR
now records that decision.

### Endpoints

| Endpoint | Purpose |
|---|---|
| `GET /invitations/{token}` | What the landing page shows: typed iD, RAiD title, service point, ORCID client name, RA contact email, expiry. 404 unknown, 410 used or expired |
| `POST /invitations/{token}/authentications` | Body `{rememberMe}` (optional). Creates a single-use OAuth state (SHA-256 hash stored, 30-minute expiry) and returns `{authorisationUrl}` at ORCID with `/read-limited /activities/update` |
| `POST /authentications` | Body `{state, code, error}` as ORCID sent them to the app's callback page. Returns outcome AUTHENTICATED or DECLINED with the signed-in and typed iDs. 400 bad state, 410 link used meanwhile, 502 ORCID failure |

Errors are problem+json with a `code` (INVITATION_NOT_FOUND,
INVITATION_USED, INVITATION_EXPIRED, INVALID_SIGN_IN_STATE,
ORCID_SIGN_IN_FAILED).

### Sign-in

- The code exchange stores the access and refresh tokens encrypted with
  AES-GCM (`TokenCipher`, the iD as associated data) and marks the invitation
  used.
- With "Remember me", the email fingerprint carried in the link is decrypted
  and stored against the iD. The latest consent wins.
- Declining at ORCID leaves the link usable.
- If the researcher signs in with a different iD from the one the minter
  typed, the authenticated iD is recorded and returned.
- Concurrent completions are safe: the state is consumed with
  `DELETE ... RETURNING`, the invitation is marked used only if not already
  used, and writes per iD hold a PostgreSQL advisory lock.
- Migration `V3__orcid_sign_in.sql` adds the `oauth_state` table and the
  token and invitation columns.

### Settings

- `ORCID_UPDATER_PUBLIC_BASE_URL` (RAID-1002) is renamed
  `ORCID_UPDATER_APP_BASE_URL`. Invitation links and the ORCID redirect URI
  (`<app>/orcid/callback`) point into the app, and only the app's origin may
  call the updater from a browser (CORS, also covering `POST /invitations`).
- New `ORCID_UPDATER_CONTACT_EMAIL`, returned to the app for its pages.

### For the app (RAID-977)

- Serve `/invitations/{token}` and `/orcid/callback`.
- Send `Referrer-Policy: no-referrer` on those pages, because the link token
  is in the page URL and would otherwise reach ORCID.

## Testing

- Unit tests: 130 pass, including `SignInControllerTest`, `SignInServiceTest`,
  `TokenCipherTest`, `OrcidOAuthClientTest`, and `SecurityConfigTest` checks
  for the researcher endpoints and CORS.
- Integration tests: 24 pass locally, including `SignInFlowIntTest` against
  PostgreSQL. CI doesn't run them yet (RAID-1010).
- The API contract validates.

## Not in this sub-task

- Writing RAiDs to ORCID (RAID-1004), including Matthias's sandbox check that
  a membership needs no organisation when an external ID is given.
- Pushing the status to the RAiD API (RAID-1005).
- Deletion and token revocation (RAID-1006).
