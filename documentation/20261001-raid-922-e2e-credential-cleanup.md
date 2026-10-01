# RAID-922: e2e credential cleanup

## What changed

The Playwright spec `raid-agency-app/e2e/tests/operator/service-point-client-credentials.spec.ts` now deletes the Keycloak client for the credential it creates, whether the test passes or fails.

- `raid-agency-app/e2e/utils/client-credential-cleanup.ts` (new) gets a fresh operator token and calls the credential SPI's delete endpoint, `DELETE /realms/raid/client-credential/delete?clientId=`. If the client ID was never recorded, it lists the group's credentials and deletes the one with the run's label.
- The spec records the group and client ID from the create call's request and response, not from the page, and deletes the client in `afterEach`.
- The UI-driven revoke in the old `finally` block is removed.

## Why

The old cleanup revoked through the UI. Revoking only disables a client, and the cleanup only ran if the credential's row had rendered. Every run left a client behind. By 2026-09-30, 48 `e2e-cred-*` clients had built up on one shared service point group in test, and 8 of them were still enabled. Enabled clients count towards that group's 10-credential cap.

Cleanup is in `afterEach`, not `finally`. When a test times out, Playwright closes the `request` context before a `finally` block runs. In local testing, a `finally` cleanup failed with "Target page, context or browser has been closed" and left the client behind.

## Dependency

The fix uses the delete endpoint added in RAID-921 (PR #688). The branch includes `feature/RAID-921` through a merge commit, so #688 must merge first.

## Testing

Local runs against a Keycloak built with the RAID-921 delete endpoint:

| Run | Test result | Clients left |
|---|---|---|
| Unmodified spec | passed | 0 |
| Timeout after create | failed (injected) | 0 |
| Error after create | failed (injected) | 0 |
| Client ID never recorded (label fallback) | failed (injected) | 0 |
| Cleanup disabled (control) | failed (injected) | 1 |

For each run, the IAM audit log showed a `credential.delete` matching the `credential.create`.

## Not in scope

The fix doesn't sweep clients left by earlier runs. The ones in test were deleted during the RAID-921 cleanup.

## Links

- JIRA: [RAID-922](https://ardc.atlassian.net/browse/RAID-922) (no parent or sub-tasks)
- PR: [#689](https://github.com/au-research/raid-au/pull/689)
- Related: [RAID-921](https://ardc.atlassian.net/browse/RAID-921) (PR [#688](https://github.com/au-research/raid-au/pull/688)), [RAID-826](https://ardc.atlassian.net/browse/RAID-826), [RAID-827](https://ardc.atlassian.net/browse/RAID-827)
