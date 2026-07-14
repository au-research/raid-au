# RAID-731 / RAID-732: Branch pipeline integration test fixes

Date: 2026-07-07

## What changed and why

The Branch-Build-Deploy pipeline (raido-root account, ap-southeast-2) was failing
its `Api-Integration-Test` stage on the `feature/RAID-712` branch deployment for
three unrelated reasons. All are now resolved and execution `2d0b7d7d` passed.

### RAID-731: DataciteErrorIntegrationTest failed without mockserver

The test created a mockserver expectation dynamically by POSTing to
`http://localhost:1080/mockserver/expectation` to make DataCite's `POST /dois`
return 429. The branch pipeline runs `./gradlew intTest` against the deployed
branch API with no local Docker stack, so the POST failed with connection
refused.

Fix (PR [#560](https://github.com/au-research/raid-au/pull/560)):

- The 429 expectation is now static in
  `api-svc/raid-api/docker-compose/mockserver/expectations.json`, matched by the
  sentinel substring `RAID-731-DATACITE-ERROR` in the request body at priority
  10 (generic 201 expectations are priority 0).
- The test mints a raid whose title contains the sentinel (the title flows
  verbatim into the DataCite payload via `DataciteTitleFactory`).
- The test skips via `Assumptions.assumeTrue` when mockserver at
  `localhost:1080` is unreachable.
- Note: mockserver loads `expectations.json` only at startup
  (`MOCKSERVER_INITIALIZATION_JSON_PATH`), so restart the container after
  pulling this change.

### RAID-732: ContributorsIntegrationTest expected AUTHENTICATED contributor

`authenticatedContributor()` asserts the test ORCID
`https://sandbox.orcid.org/0009-0002-5128-5184` has status `AUTHENTICATED`, but
the record was `AUTHENTICATION_REVOKED`. Contributor auth status is served from
the **ContributorRaid** DynamoDB table in the raid-demo account (856329357653,
us-east-1) — the test environment points at an API endpoint in that
environment. Data fix, no code change: the metadata item
(`PK=CONTRIBUTOR#0009-0002-5128-5184`, `SK=METADATA`) was updated from
`AUTHENTICATION_REVOKED` to `AUTHENTICATED` with a conditional write.

Left as-is (possible follow-up tickets):

- A second metadata item with a malformed key (`CONTRIBUTOR0009-...`, missing
  the `#` separator) still has `AUTHENTICATION_REVOKED`.
- In the demo Postgres `api_svc.contributor` table the same ORCID exists as
  three rows under different PID prefixes (bare / orcid.org / sandbox.orcid.org)
  with independent statuses, because uniqueness is on `(pid, schema_id)`.

### Stale iam image in the test environment

Seven `GroupServicePointAdminIntegrationTest` tests failed with 404
"Could not find role" because CI had redeployed `iam-service` from main
(revision 957, commit `b8730f72`), superseding the locally built RAID-712 SPI
image (revision 955). Because `feature/RAID-712` did not contain `b8730f72`
(RAiD-710 refresh token), a plain rollback would have regressed the test
environment. Instead:

1. Merged `origin/main` into `feature/RAID-712` (merge commit `c104aad2`, clean).
2. Rebuilt and redeployed the iam image from the merged branch using the new
   script `scripts/deploy-iam-to-test.sh` (task definition revision 958).

## JIRA

- [RAID-731](https://ardc.atlassian.net/browse/RAID-731) — Done
- [RAID-732](https://ardc.atlassian.net/browse/RAID-732) — Done
- [RAID-712](https://ardc.atlassian.net/browse/RAID-712) — parent story, Done

## PRs

- [#558](https://github.com/au-research/raid-au/pull/558) — RAID-712 (`feature/RAID-712` → `main`), merged; included the RAID-731 fix
- [#560](https://github.com/au-research/raid-au/pull/560) — RAID-731 standalone PR, closed unmerged (superseded by #558)

## Post-merge rollout (2026-07-13/14)

- Image `5748efa9` (the #558 merge) deployed to test and demo by Build-Push-Deploy-V2
- Demo verified healthy and backward-compatible (flat group-admin fallback active)
- Scoped-role migration endpoint run: test (already fully migrated, idempotency
  confirmed) and demo (49 roles created, 92 grants added, idempotency confirmed)
- Stage and prod migration runs to follow their deployments
