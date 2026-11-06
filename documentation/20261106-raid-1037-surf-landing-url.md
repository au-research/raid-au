# RAID-1037: SURF RAiD resolves to static.api_user.raid.org.au (findings)

- JIRA: [RAID-1037](https://ardc.atlassian.net/browse/RAID-1037) (Spike)
- Related: companion tickets on the landing page links and the `landing-prefix` default (raised with RAID-1037)
- Status: findings for review, 6 November 2026
- PR: [#775](https://github.com/au-research/raid-au/pull/775)

Labels used below: **Verified** means checked on 6 November 2026 against
`origin/main` at `be3b6ece`, a live public endpoint, or one of the two probes
in this PR. **Unverified** means an inference that still needs SURF to
confirm it. SURF's configuration has not been seen.

## Summary

SURF's instance is running with `raid.environment=api_user` and no explicit
`raid.identifier.landing-prefix`. The default prefix,
`https://static.${raid.environment}.raid.org.au/raids/`, therefore became
`https://static.api_user.raid.org.au/raids/`. That URL was registered with
DataCite as the DOI target and stored on the RAiD as
`identifier.raidAgencyUrl`.

The fix for SURF needs no SQL and no change to `raid.environment`:

1. Set `raid.identifier.landing-prefix=https://static.raid.surf.nl/raids/`
   and restart the API.
2. Send one `PUT /raid/10.83338/831b5565` with the current record, but with
   `identifier.raidAgencyUrl` set to
   `https://static.raid.surf.nl/raids/10.83338/831b5565`.

Step 2 creates version 2 of the RAiD with the corrected URL and re-sends the
DOI to DataCite with the new target. Both steps were proven on a local stack.
Changing `raid.environment` to `prod` was proven unsafe (see
[Why SURF should not change raid.environment](#4-why-surf-should-not-change-raidenvironment)).

## 1. What is wrong, and where

- **Verified.** `https://raid.org/10.83338/831b5565` returns `301` and
  `https://doi.org/10.83338/831b5565` returns `302`, both to
  `https://static.api_user.raid.org.au/raids/10.83338/831b5565`.
- **Verified.** DataCite's public API
  (`https://api.datacite.org/dois/10.83338/831b5565`) holds that URL as
  `attributes.url`. The DOI is `findable` and was created on
  5 November 2026.
- **Verified.** SURF's landing page
  (`https://static.raid.surf.nl/raids/10.83338/831b5565/`) embeds the RAiD
  record. It shows `identifier.raidAgencyUrl` as the `api_user` URL at
  `identifier.version` 1, so the wrong value is stored in SURF's
  `raid_history` and served by the API. `identifier.id` is correct
  (`https://raid.org/10.83338/831b5565`), so `name-prefix` is set correctly.

## 2. How `api_user` reached `raid.environment`

- **Verified.** `raid.environment` defaults to `not-set`
  (`application.yaml` line 2). Left alone, the landing URL would have been
  `https://static.not-set.raid.org.au/...`. The `api_user` host can only come
  from `raid.environment` being set to `api_user` explicitly.
- **Verified.** `api_user` looks like an environment name in several places
  an agency would read:
  - `api-svc/db/src/main/resources/db/env/` holds `api_user` next to `demo`,
    `dev`, `prod`, `stage` and `test`.
  - The default `spring.flyway.locations` is
    `classpath:db/env/api_user,classpath:db/migration,classpath:db/env/${raid.environment}`.
  - `doc/reference/deployment-configuration.md` uses `raid.db.user = api_user`
    in its worked example.
- **Verified.** On a fresh database nothing creates the `api_user` role.
  `db/env/api_user/V2__create_api_user.sql` is below `baseline-version: 25`,
  so Flyway skips it, and `B25__baseline.sql` does not mention `api_user`
  (probe 1, step 1: V2 not applied, role absent).
- **Verified.** `raid.environment=api_user` starts cleanly. The folder
  exists, Flyway discards the duplicate location with a warning, and nothing
  validates the value.
- **Unverified.** The likely sequence is that SURF needed an `api_user` role
  for `raid.db.user`, found `db/env/api_user`, set `raid.environment=api_user`
  to try to get it created, and did not revert the setting once the role
  existed. Only SURF can confirm this.

## 3. How the landing URL is used

- **Verified.** `IdFactory` builds `identifier.raidAgencyUrl` from
  `landing-prefix` + handle at mint. The value is then stored in
  `raid_history` and in the materialised `raid.metadata` column.
- **Verified.** On update, `RaidHistoryService.save` stores the
  `raidAgencyUrl` the client sends. It is not rebuilt from configuration.
  `GET /raid/{handle}` reads from `raid_history`, and `/raid/all-public` (the
  static site's source) reads `raid.metadata` through `RaidDtoReadService`.
- **Verified.** `DataciteAttributesDtoFactory` builds the DataCite `url`
  attribute from the current `landing-prefix` on every push (mint, update,
  `post-to-datacite` and resync).
- **Verified.** `RaidService.update` compares checksums of the whole record
  (excluding `metadata`) and returns early when they match, without saving
  or calling DataCite.

So correcting `landing-prefix` alone fixes nothing for an existing RAiD.
The DOI target is only corrected when something pushes to DataCite, and the
stored `raidAgencyUrl` is only corrected when a client sends a new value.

## 4. Why SURF should not change raid.environment

Probe 1 (`api-svc/db/probe/raid-1037-environment-switch/`) builds a database
with `raid.environment=api_user`, seeds one `raid_history` row carrying the
`api_user` URL, then re-runs Flyway with the locations `prod` would give.

- **Verified.** Switching to `prod` fails startup validation:
  `Detected resolved migration not applied to database` for 34.1, 36.1,
  36.2, 39.1, 40.1 and 41.1. These are below the database's current V49.
- **Verified.** Forcing it with `outOfOrder=true` succeeds, but `V36.1`
  rewrites the seeded `raidAgencyUrl` to
  `https://static.prod.raid.org.au/raids/10.83338/831b5565`, a RAiD AU host.
  The other prod-folder migrations are RAiD AU data repairs and should not
  run against SURF's data either.

`raid.environment=api_user` has one side effect worth knowing: the guard
that refuses `raid.stub.datacite.enabled=true` only fires when the value is
`prod`. The stub is off by default, so this matters only if SURF enables it.
SURF should leave `raid.environment` as it is.

## 5. Correcting the minted RAiD

Probe 2 (`api-svc/raid-api/probe/raid-1037-landing-url-fix/`) runs the API
from this branch against the local dev stack, with DataCite served by
MockServer. It mints a RAiD with the `api_user` prefix, restarts the API with
`https://static.raid.surf.nl/raids/`, then tries the candidate fixes.

| Step | Result | |
|---|---|---|
| GET after the config change | Still the `api_user` URL, version 1 | **Verified** |
| PUT the record back unchanged | `200`, no new version, no DataCite call | **Verified** |
| PUT with `identifier.raidAgencyUrl` corrected | `200`, version 2 whose diff changes only `raidAgencyUrl`; DataCite `PUT /dois/<handle>` with the SURF URL | **Verified** |
| GET and `raid.metadata` afterwards | SURF URL, version 2 | **Verified** |

Other routes, and why they are not recommended:

- **Editing in the RAiD app.** **Verified.** The app sends back the
  identifier it read (`raidRequest` in
  `raid-agency-app/src/utils/data-utils/data-utils.ts`), including the old
  `raidAgencyUrl`. Unless another field changes, the checksum matches and nothing happens. If
  another field changes, DataCite is corrected but the stored
  `raidAgencyUrl` stays wrong.
- **`DataciteResyncWorker`** (`raid.datacite.resync.enabled`). It re-pushes
  rows flagged with `raid.datacite_resync_required`, which needs a manual SQL
  `UPDATE` to set. It corrects DataCite only, not the stored
  `raidAgencyUrl`.
- **`POST /raid/post-to-datacite`.** Needs the `raid-upgrader` role and also
  corrects DataCite only.
- **SQL on `raid_history`** (as `V36.1` does for RAiD AU). Works, but is a
  manual data change on another agency's database, and the PUT already does
  the same thing through the API with an audit trail in the history.

**Unverified.** DataCite was mocked, so the probe proves what the API sends,
not that DataCite accepts it. The request is the same full-document
`PUT /dois/{doi}` the API already uses for every update in production, so
the risk is low. SURF can confirm afterwards by checking
`https://api.datacite.org/dois/10.83338/831b5565` and the redirect from
`https://raid.org/10.83338/831b5565`.

## 6. Steps for SURF

1. Confirm the current values of `raid.environment`,
   `raid.identifier.landing-prefix` and `raid.identifier.name-prefix`, and
   how the `api_user` role was created.
2. Set `raid.identifier.landing-prefix=https://static.raid.surf.nl/raids/`.
   Leave `raid.environment` unchanged. Restart the API.
3. `GET /raid/10.83338/831b5565`, change `identifier.raidAgencyUrl` to
   `https://static.raid.surf.nl/raids/10.83338/831b5565`, and `PUT` the
   result back to `/raid/10.83338/831b5565` with a token for the owning
   service point (or an operator).
4. Check that the response shows version 2 with the new URL, that DataCite's
   `url` has changed, and that `https://raid.org/10.83338/831b5565`
   redirects to `https://static.raid.surf.nl/raids/10.83338/831b5565`.
5. Rebuild the static site so the landing page picks up version 2.

Any further RAiDs minted before step 2 need the same PUT.

## Found, not changed

Outside this ticket's scope. To be raised as separate tickets:

- Nothing creates the `api_user` role on a fresh database, while the
  deployment reference names `api_user` as the database user.
- `raid.environment` is not validated at startup, so a value such as
  `api_user` or `not-set` starts silently.
- The API stores whatever `identifier.raidAgencyUrl` the client sends on
  update, rather than deriving it from `landing-prefix`.
- SURF's landing page shows the RAiD as `https://raid.surf.nl10.83338/831b5565`
  (missing `/`) and links "Show in RAiD App" to `app.prod.raid.org.au`. These
  probably belong to the companion landing page ticket.

## Evidence

- `api-svc/db/probe/raid-1037-environment-switch/`: Flyway environment
  switch probe (Docker only).
- `api-svc/raid-api/probe/raid-1037-landing-url-fix/`: mint-then-fix
  verification against the local dev stack.

Nothing was created or changed in any shared environment. The local Docker
stack used for probe 2 was removed afterwards.
