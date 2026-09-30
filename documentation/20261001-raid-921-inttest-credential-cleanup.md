# RAID-921: delete credential clients created by the integration test

- JIRA: [RAID-921](https://ardc.atlassian.net/browse/RAID-921) (Bug)
- Related: [RAID-922](https://ardc.atlassian.net/browse/RAID-922) (the same leak in the e2e suite)
- PR: [#688](https://github.com/au-research/raid-au/pull/688)

## Problem

`ClientCredentialIntegrationTest` mints scoped client credentials through the IAM credential SPI and never removed the Keycloak clients it created. Each run left about 42 clients named after the test labels, such as `peek`, `doomed` and `cap-0` to `cap-9`. There were two causes:

- Revoke only disables a client (`setEnabled(false)`), by design.
- The test deletes its per-test groups, but deleting a group does not delete credential clients. Clients link to their group only through the `raid.credential.group-id` attribute.

By 2026-09-30 the test Keycloak held 2,733 of these clients from about 71 runs, and 2,344 of them were still enabled with working secrets.

## What changed and why

The test had no way to delete a client. The SPI had no delete operation, and `integration-test-client` holds only `manage-users` and `view-realm`. There were two options:

1. **Grant `manage-clients` to `integration-test-client`.** Rejected: it needs a manual grant in each environment, and it gives a test client control of every client in the realm.
2. **Add a delete operation to the credential SPI.** Chosen. It needs no permission changes and works the same in every environment. The existing lifecycle ADR (`doc/adr/2026-08-27_in-process-spi-credential-lifecycle.md`) already allows `removeClient` as well as disabling, so no new ADR was needed.

### IAM

- New `DELETE /realms/raid/client-credential/delete?clientId=…`, with an `OPTIONS` preflight, in `ClientCredentialController`.
- It uses `ClientManager.removeClient`, the same path as the Admin API's client delete. In keycloak-services 26.5.6 this removes the client, its user and auth sessions, and its service account user. It also refuses Keycloak's internal clients.
- Authorisation is identical to revoke: operator, or `service-point-admin:<groupId>`. The flat `group-admin` role is rejected, and non-managed clients return 404.
- Responses: `204` on success; `404` for an unknown or already-deleted credential, including a concurrent-delete race; `400` for a blank `clientId`.
- New audit action `credential.delete`.

### Integration test

- The test tracks every credential it creates and deletes them all in the outer `@AfterEach`, before the operator user is deleted.
- Cleanup is best-effort, so a run against an IAM without the endpoint still passes.
- The DataApiAccess nested class's own revoke teardown is removed because the shared cleanup now covers it.
- New `Deletion` tests: delete removes the client and its service account; a revoked credential can be deleted; deleting twice returns 404; a cross-service-point delete is denied and the credential survives.

### Documentation

- `doc/reference/service-point-client-credentials.md`: new "Step 8: Delete a credential".

## Verification

Run locally against a freshly rebuilt IAM image:

- `iam` unit tests: 202 passed, 0 failed.
- `ClientCredentialIntegrationTest`: 29 of 29 passed. Afterwards the realm contained 0 `raid-cred-*` clients and 0 credential service accounts.
- Full `intTest`: 254 tests, 0 failures, 16 pre-existing skips in unrelated classes. This run was before the review follow-ups, which changed only the delete endpoint's response for a race and the test assertions.

## Environment cleanup (2026-09-30)

A one-off Admin API script deleted 2,781 credential clients from the test Keycloak with 0 failures:

| Source | Count |
| --- | --- |
| Integration test clients whose group no longer existed | 2,628 |
| Disabled `data api - *` test clients on the shared fixture groups | 105 |
| `e2e-cred-*` clients from the Playwright e2e suite (RAID-922) | 48 |

One apparently real credential (`matthias-cc`) was kept.

## Follow-ups

- **Deploy order.** The branch pipeline tests against the shared test IAM, which is deployed from `main`. The new `Deletion` tests fail there until this is merged and IAM is redeployed, or until the branch IAM is deployed with `scripts/deploy-iam-to-test.sh`.
- **RAID-922.** The e2e suite should call this endpoint in its teardown.
- **Not ticketed yet:**
  - `iam/doc/spis.md` lists none of the client-credential endpoints.
  - `TokenService.getClientToken` in the intTest sources logs the client secret at DEBUG level.
  - Deleting a group still leaves its credentials behind.
