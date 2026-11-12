# RAID-1056: Call the RA's ORCID updater instead of the central API

- JIRA: [RAID-1056](https://ardc.atlassian.net/browse/RAID-1056), sub-task of [RAID-976](https://ardc.atlassian.net/browse/RAID-976) (epic [RAID-974](https://ardc.atlassian.net/browse/RAID-974))
- Branch: `feature/RAID-1056` into `feature/RAID-976`
- PR: to follow
- Status: in review

## What changed and why

Each registration agency now runs its own ORCID updater (RAID-975), so the RAiD
API tells that updater about saved RAiDs instead of the central ORCID
Integration API.

- After every mint, update and patch, the API sends the RAiD's handle to
  `POST {raid.orcid-integration.host}/raid-changes` with the `X-Api-Key` header.
  The updater reads the RAiD back from the API and does the ORCID work itself.
- The call waits until the transaction commits, so the updater never reads a
  version that was rolled back or not yet saved. It runs in the background.
- A failed call is logged at `ERROR` and the save still succeeds. Before this
  change, an unreachable central updater failed the save after the DataCite
  mint. The next save of the RAiD tells the updater again.
- `raid.orcid-integration.api-key` is now required when the integration is
  enabled. Its default changed from `not-set` to empty, and startup fails naming
  the property when it is missing, as it already did for `host`.
- The central API's message (`RaidListenerMessage` and its factory) and the
  `raid.orcid-integration.raid-listener.uri` setting are removed. The new setting
  is `raid.orcid-integration.raid-changes.uri`, which defaults to
  `${host}/raid-changes`.
- Local development: the MockServer expectation moved from `/raid-update` to
  `/raid-changes`, and the dev profile sets an API key.

Contributor status lookups still use the old `find-contributor-by-orcid`
endpoint. RAID-1057 replaces them with the status stored in the API.

## Deployment impact

A deployment with `raid.orcid-integration.enabled=true` must point `host` at its
own ORCID updater and set `api-key`, or the API will not start. RAiD AU's test,
demo, stage and prod environments enable the integration against the central
host today, so this cannot be released until RAiD AU's updater is deployed
(RAID-1008) and its configuration updated. This is the RAID-978 cutover for
RAiD AU.

Branch environments: the companion
[raido-v2-aws-private#80](https://github.com/au-research/raido-v2-aws-private/pull/80)
turns the integration off for branch APIs with this code. Branches have no ORCID
updater: an updater serves one app and one API, and each app's ORCID redirect
URI takes ORCID more than a week to register. A single updater in the test
environment checks the ORCID flow after merging to `main`. Branches with older
API code keep the central host. The companion must be merged, and the admin
stack deployed, before this branch is pushed.

## Testing

- Unit tests: `OrcidIntegrationServiceTest` (no call when disabled; sent
  straight away with no transaction; held until commit; not sent on rollback; a
  failure is not thrown; startup refuses a missing API key),
  `OrcidIntegrationClientTest` and `OrcidIntegrationPropertiesTest`.
- New intTest `OrcidUpdaterNotificationMockIntegrationTest`: minting and updating
  a RAiD each record a `POST /raid-changes` with the RAiD's handle and the API
  key header in MockServer.
- Full intTest locally: 267 tests, 0 failures, 16 skipped.

## Documentation

- `doc/reference/deployment-configuration.md`: the `host` and `api-key`
  properties, the background notification and the startup check.
- `api-svc/doc/orcid-integration.md` still describes the central updater. It is
  rewritten once RAID-976's sub-tasks are done.
