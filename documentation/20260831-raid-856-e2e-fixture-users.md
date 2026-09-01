# RAID-856: Branch pipeline E2e-Test stage restored

**Date:** 2026-08-31
**JIRA:** [RAID-856](https://ardc.atlassian.net/browse/RAID-856)
**PRs:** raid-au [#636](https://github.com/au-research/raid-au/pull/636), raido-v2-aws-private [#44](https://github.com/au-research/raido-v2-aws-private/pull/44)

## What was wrong

The Branch-Build-Deploy `E2e-Test` stage had failed on **every** execution since `000d6af8`
("Add e2e tests for RAID-659, RAID-480 and RAID-608") merged on 2026-08-24. Three separate gaps,
all from the same root cause: the e2e suite gained environment-dependent fixtures and nothing
outside local dev was given them.

### 1. Missing credentials (2 hard failures)

Both new Playwright auth-setup projects threw immediately:

```
Error: VITE_KEYCLOAK_E2E_OPERATOR_USER environment variable is not set.
Error: VITE_KEYCLOAK_E2E_UNAPPROVED_ADMIN_USER environment variable is not set.
```

A failed setup project fails the stage *and* skips every spec depending on it — 2 failed,
3 never ran, 28 passed.

### 2. Missing fixture users

The three users documented in `iam/doc/keycloak-configuration.md` existed only in the local dev
realm. Verified missing from `iam.test.raid.org.au`: `raid-operator`,
`raid-au-unapproved-admin`, `raid-au-pending-user`.

### 3. A test that could only pass locally

`service-point-group-id-error.spec.ts` targeted the `raido` service point assuming its `group_id`
is `169bd3f3…` — a pairing set by `db/env/dev/V32.1__update_service_point_repository.sql`. Branch
and test environments run `db/migration,db/env/test` and never apply it. There, `raido` has a null
`groupId` and `169bd3f3…` belongs to the `RAiD AU (branch-…)` service point created at deploy time.

## What changed

- **Test realm** — provisioned the three fixture users with exactly the documented roles and
  membership. `raid-au-unapproved-admin` is deliberately a raw, self-joined member of `raid-au`
  with `group-admin` but **no** `service-point-user`; that unapproved state is the whole point of
  the RAID-608 regression test. Verified: it gets a 401 from `PUT /group/grant`, and the operator
  can list the group's members and see `raid-au-pending-user` among them.
- **Secrets** — `raid_operator_password` and `raid_au_unapproved_admin_password`, defined in
  `AdminSecrets` alongside the existing per-fixture-user secrets.
- **Buildspec** — the four missing `VITE_KEYCLOAK_E2E_*` variables, plus read grants.
- **The test** — now discovers which group to break from the running environment. A valid group
  cell's tooltip is the group id itself, so it breaks the first resolvable row's lookup and uses
  the second as the control, tracking rows by DataGrid `data-id` rather than display name.

## Verification

Run against the deployed branch environment with exactly the env the buildspec supplies:

| Stage | Result |
| --- | --- |
| Before any fix | 2 failed, 3 did not run, 28 passed |
| After the credentials fix | 1 failed, 33 passed |
| After the test fix | **34 passed** |

CDK: `npm run build` clean, 153 tests passing. Frontend unit tests: 147 passing.

## Not resolved

`cdk diff` / `deploy` of the Admin stacks fails with `Unable to resolve AWS account to use`. This
reproduces on unmodified `main`, so it is pre-existing and independent — but the buildspec change
cannot reach the pipeline until it is sorted out, which means the E2e stage stays red in CI even
though the suite itself is now green.

## Lesson

This is the same shape as the resolver-stub CDK companion problem: a change lands in `raid-au`
that needs a matching change in `raido-v2-aws-private`, and without it the deployed environments
silently diverge from local dev. Worth a checklist item for changes adding environment-dependent
test fixtures.
