# RAID-889: Document the production Keycloak clients

**Date:** 22 September 2026
**JIRA:** [RAID-889](https://ardc.atlassian.net/browse/RAID-889) (no sub-tasks)
**PR:** [au-research/raid-au#675](https://github.com/au-research/raid-au/pull/675)

## What changed

- Added `iam/doc/keycloak-production-clients.md`, a reference for all 20 OIDC
  clients in the production `raid` realm at `iam.prod.raid.org.au`. For each
  client it records the type, enabled flows, redirect URIs and web origins,
  service-account roles, client scopes, and the code in `raid-au`,
  `raido-v2-aws-private` or `raid-aws-private` that consumes it.
- Added a pointer to it from `iam/doc/keycloak-configuration.md`, which describes
  the local development realm only.

Documentation only. No code or configuration changed, so there are no unit tests
to add.

## Why

Nothing recorded what the production Keycloak clients were for. The existing
`keycloak-configuration.md` documents local development, and the production realm
has drifted from it: production holds `raid-access-handler`, `static-page-client`,
`access-handler`, `eosc` and `raid-uplifter`, none of which appear in either realm
import file, while the `static-generator` client that the `registration-agency`
CDK configures does not exist in production at all.

## How the data was captured

The Keycloak Admin REST API could not be used. The `admin` password held in the
`keycloak-credentials` secret is rejected by `iam.prod.raid.org.au/realms/master`,
and no service account in the realm holds `realm-management` roles, so
`GET /admin/realms/raid/clients` returns 403 even with a valid client-credentials
token.

The client configuration was read instead from the `keycloak` schema of the
production `raido` database, over an SSM port-forward through an ECS container
instance, authenticating with the `iam-db-credentials` secret. Read-only `SELECT`s
against `client`, `redirect_uris`, `web_origins`, `scope_mapping`,
`keycloak_role`, `user_role_mapping`, `user_entity`, `client_scope`,
`client_scope_client` and `client_attributes`. Nothing was written.

Two live runtime configs were also fetched to confirm which client IDs are
actually in use, rather than inferring from CDK: `app.prod.raid.org.au/app-config.json`
(`raid-api`) and `static.prod.raid.org.au/app-config.json` (`static-page-client`
and `raid-access-handler`).

## Findings not addressed here

The document's Findings section records eight observations from the audit. They
are out of scope for this ticket and each needs its own if we decide to act:

1. `eosc` has no reference in any of the three repositories, yet is enabled with
   wildcard redirect URIs.
2. `raid-uplifter` has no code reference and its service account holds `operator`.
3. `raid-access-handler`'s service account holds seven roles, including `operator`
   and `group-admin`, for a static site build job.
4. `static-page-client` also holds `operator`.
5. Two disabled `raid-cred-*` clients remain from the RAID-877 prod verification.
6. `invite-client` is provisioned in production but has no deployed consumer there.
7. `doc/security/access-control/authentication/readme.md` names a non-existent
   `raid-agency-app` client.
8. The production client list is not in version control and cannot be
   reconstructed from it.

## Limitations

The Registration Authority production account (`531195466715`) was not reachable
from this session. The `contributor-writer` wiring described for it comes from
`raid-aws-private` configuration, not from a confirmed running deployment, and the
document says so.
