# RAID-1031: ORCID client public API access and docs

- JIRA: [RAID-1031](https://ardc.atlassian.net/browse/RAID-1031) (Task, no parent)
- PR: [#771](https://github.com/au-research/raid-au/pull/771), merged to `main` on 2026-11-05 (`5ddcf487`)
- Status: Dev Complete. Not yet released to production.

## Why

SURF (NL) couldn't mint their first production RAiD on 2.20.0. Every request with
an ORCID contributor failed with:

```
External resolver(s) unavailable: contributor[0].id -> ORCID(401)
```

They had set `raid.orcid-integration.enabled=false` and commented out
`raid.orcid-client.base-url` and the access token, expecting that to stop all
ORCID calls. It doesn't. Contributor validation (`OrcidClient.exists()`) and
DataCite creator names (`OrcidClient.getName()`) call ORCID directly using
`raid.orcid-client.*`, whatever the integration setting. With those commented
out, the defaults applied: the ORCID sandbox member API and the token `not-set`,
which ORCID rejects with 401.

Two things made this easy to get wrong:

- `OrcidRequestEntityFactory` always sent `Authorization: Bearer <token>`. ORCID's
  public API (`pub.orcid.org`) accepts anonymous reads but rejects
  `Bearer not-set`.
- `doc/reference/deployment-configuration.md` called `api.orcid.org` the "public
  API". It's the member API, which refuses anonymous requests. The docs also
  didn't say that ORCID lookups are separate from the integration.

## What changed

- **Code:** `OrcidRequestEntityFactory` sends no `Authorization` header when
  `raid.orcid-client.access-token` is unset, blank or `not-set`.
- **Tests:** `OrcidRequestEntityFactoryTest` rewritten to cover token present,
  null, empty, whitespace, `not-set` and ` not-set `, for both HEAD and GET.
- **Docs:** new "ORCID iD lookups" section in `deployment-configuration.md`. It
  covers the 401 symptom, which host and token to use for each type of ORCID
  credential, ORCID's limits and terms, and the workaround for 2.20.1 and earlier
  (set the token to empty). Corrected the host and token wording in the
  external resolvers, secrets and all-properties tables.

## Verification

- Unit tests: 12 passing.
- Branch pipeline on `4f1da155`: build, deploy, API integration tests and e2e
  tests all passed. The branch environment runs with `raid.stub.orcid.enabled=true`,
  so it doesn't exercise the header change.
- Live check against `https://pub.orcid.org/v3.0/` (throwaway test, not
  committed), using the same `RestTemplate` setup as `Api.java`:
  - New code with `not-set`, null or empty token: iD found, unknown iD returns
    not found, name returned.
  - Old behaviour with `Bearer not-set`: 401, which reproduces SURF's error.
  - Old behaviour with `Bearer ` (empty token): HEAD 200 and personal details
    returned, which backs the documented workaround.

ORCID host behaviour (curl, 2026-11-05):

| Host | No token | `Bearer not-set` |
|---|---|---|
| `pub.orcid.org/v3.0` | 200 | 401 |
| `api.orcid.org/v3.0` | 403 | 401 |
| `api.sandbox.orcid.org/v3.0` | 403 | 401 |

## Not verified or out of scope

- Whether a token from ORCID public API credentials works against
  `api.orcid.org`. The docs only pair public credentials with `pub.orcid.org`.
- Anonymous access to the sandbox public API (`pub.sandbox.orcid.org`). That
  row was removed from the docs table.
- A startup warning when `base-url` is still the sandbox outside sandbox and dev
  environments, and changing the `base-url` default. Not ticketed.
