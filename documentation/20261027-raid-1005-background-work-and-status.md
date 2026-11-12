# RAID-1005: Background work, status propagation and the updater's own credentials

- JIRA: [RAID-1005](https://ardc.atlassian.net/browse/RAID-1005), sub-task of [RAID-975](https://ardc.atlassian.net/browse/RAID-975) (epic [RAID-974](https://ardc.atlassian.net/browse/RAID-974))
- Earlier sub-tasks: [RAID-1000](https://ardc.atlassian.net/browse/RAID-1000) (PR #741), [RAID-1001](https://ardc.atlassian.net/browse/RAID-1001) (PR #744), [RAID-1002](https://ardc.atlassian.net/browse/RAID-1002) (PR #745), [RAID-1003](https://ardc.atlassian.net/browse/RAID-1003) (PR #746), [RAID-1004](https://ardc.atlassian.net/browse/RAID-1004) (PR #747)
- Related: [RAID-1011](https://ardc.atlassian.net/browse/RAID-1011) creates the updater's Keycloak client at boot; [RAID-976](https://ardc.atlassian.net/browse/RAID-976) builds `PUT /contributor/status` and the "RAiD changed" call in the RAiD API
- PR: [#748](https://github.com/au-research/raid-au/pull/748) (targets `feature/RAID-975`)
- Status: merged into `feature/RAID-975` on 2026-10-27; RAID-1005 Dev Complete

## What changed and why

RAID-1004 can write a RAiD to ORCID, but nothing called it. This sub-task
connects the updater to the RAiD API in both directions, with the decisions
Rob agreed on 2026-10-27:

- `POST /raid-changes` queues the RAiD and answers 202. A background worker
  reads the RAiD from the RAiD API and calls the writer.
- An embargoed RAiD (any access type other than open) is held back. It is
  written when the RAiD API saves it as open, which RAiD AU's access handler
  does when the embargo ends. The updater keeps no embargo timer.
- Work is queued in a database table (`work_item`, migration V5) rather than
  run on the request thread. Every instance polls it with
  `FOR UPDATE SKIP LOCKED` and a claim timeout, so nothing is lost on a
  restart and instances can scale out. Requests for the same RAiD or iD are
  merged. When the RAiD API or Keycloak can't be reached, an item is tried
  again with back-off (5 attempts from 30 seconds), then kept with
  `failed_at` and `failure` for operators.
- Status changes are pushed to the RAiD API with `PUT /contributor/status`:
  invitation sent, sign-in, token rejected by ORCID, and invitation expired
  (found by a sweep on each poll). The worker works out the status from what
  the updater holds, so a late item never downgrades anyone. On sign-in,
  every RAiD naming the researcher is queued for writing.
- `GET /contributors/status` answers the RAiD API. When a researcher signed
  in as a different iD from the one the minter typed, it returns
  AUTHENTICATED for the typed iD with `authenticatedId` set, so RAID-976 can
  replace it.
- The updater calls the RAiD API with its own Keycloak client and
  `private_key_jwt`, so no client secret exists. It creates an RSA key on
  first start, keeps the private half encrypted in `service_key`, and
  publishes the public half at `/.well-known/jwks.json`. RAID-1011 will
  create the Keycloak client pointing at that URL, which meets Rob's
  requirement that credentials are created without manual steps.

ORCID writes are still not retried in the background (decision Q6). Only
calls to the RAiD API and Keycloak are, because the work can't start without
them.

### Settings

| Environment variable | Default |
|---|---|
| `ORCID_UPDATER_RAID_API_CLIENT_ID` | `orcid-updater` |
| `ORCID_UPDATER_WORK_POLL_INTERVAL` | `5s` |
| `ORCID_UPDATER_WORK_BATCH_SIZE` | `20` |
| `ORCID_UPDATER_WORK_MAX_ATTEMPTS` | `5` |
| `ORCID_UPDATER_WORK_INITIAL_BACKOFF` | `30s` |
| `ORCID_UPDATER_WORK_CLAIM_TIMEOUT` | `5m` |

### Dependencies not yet built

- `PUT /contributor/status`, `GET /raid/?contributor.id=` access for the
  updater's client, and the "RAiD changed" call are RAID-976. The updater is
  built against the agreed contract and tested with the RAiD API stubbed.
- The Keycloak client is RAID-1011 (see
  `20261028-raid-1011-orcid-updater-keycloak-client.md`). Without it a
  deployed updater gets `invalid_client` from Keycloak and its work items
  retry, then fail for operators.

## Testing

- Unit tests: 214 pass. New: ContributorStatusService, WorkProcessor,
  WorkScheduler, RaidServiceClient, ServiceTokenClient, ServiceKeyCipher,
  and the two RAiD API endpoints. Existing service tests check each hook
  queues the right work.
- Integration tests: 45 pass, 1 skipped (the manual ORCID sandbox test).
  New:
  - `WorkQueueIntTest` (13) against PostgreSQL: merging, claiming, the
    generation check, failures, the expiry sweep, sign-in through to an
    ORCID entry with its put code, the embargo hold, back-off after the
    RAiD API fails, the stored service key and the JWKS endpoint.
  - `ServiceTokenKeycloakIntTest` (2) against Keycloak 26.6.2 in
    Testcontainers: a client created with `client-jwt` and the updater's
    JWKS URL issues a token for the updater's signed assertion, and refuses
    one signed with an unknown key (`invalid_client`).
  - The sign-in and invitation flow tests check the queued work, and
    `GET /contributors/status` returns the signed-in iD for a typed one.
- Build with `--release 17` passes.
- The contract validates with `openApiValidate`.

## Documentation

- `orcid-updater/README.md`: background work, the updater's Keycloak client
  and JWKS URL, and the new settings.
- `orcid-updater/idl/orcid-updater-openapi-3.1.yaml`: `getJwks`, and
  `notifyRaidChanged` described as queued.
- `doc/adr/2026-10-23_per-ra-orcid-updater.md`: `private_key_jwt` under
  "Callers and credentials", and a new section "Background work and status
  propagation".
