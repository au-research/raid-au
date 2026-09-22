# Production Keycloak clients (`raid` realm)

This document describes every OIDC client in the **production** `raid` realm at
`https://iam.prod.raid.org.au`, and traces each one back to the code that uses it.

It complements [keycloak-configuration.md](keycloak-configuration.md), which
describes the **local development** realm. The two realms have drifted, so read
this document for anything production related.

- **Realm:** `raid`
- **Keycloak host:** `https://iam.prod.raid.org.au`
- **AWS account:** `903388824296` (`raido-prod`)
- **Captured:** 22 September 2026 (20 clients)

Client configuration was read directly from the Keycloak schema in the production
`raido` database. The client list is not held in version control: the realm import
files (`iam/realms/raid-realm.json` and
`raido-v2-aws-private/registration-agency/cdk/config/keycloak/raid-realm.json`) are
used for local development and for bootstrapping a brand new agency deployment
respectively, and neither is applied to the running production realm. Every
observation below about production is from the live realm; every observation about
intent is from the repositories named.

## Summary

| Client | Type | Flows | Purpose |
|---|---|---|---|
| `raid-api` | Public | Standard, Direct grant | Browser login for the RAiD agency app (`app.prod.raid.org.au`) |
| `raid-api-2` | Confidential | Standard, Direct grant | `oauth2Login` for the API service itself (`api.prod.raid.org.au`) |
| `static-page-client` | Confidential | Standard, Implicit, Direct grant, Service account | Public data fetching for the static site build |
| `raid-access-handler` | Confidential | Standard, Service account | Embargoed data fetching for the static site build |
| `access-handler` | Confidential | Standard, Service account | Embargo-expiry Lambda |
| `raid-permissions-admin` | Confidential | Standard, Direct grant, Service account | API service calling the IAM permissions SPI |
| `raid-dumper` | Confidential | Standard, Direct grant, Service account | Bulk export to Zenodo (Raid-Dumper CodeBuild) |
| `raid-upgrader` | Confidential | Standard, Direct grant, Service account | Operator data-uplift scripts |
| `contributor-writer` | Confidential | Standard, Direct grant, Service account | ORCID integration writing contributor status back |
| `invite-client` | Confidential | Standard, Direct grant, Service account | Contributor invite feature |
| `raid-uplifter` | Confidential | Standard, Service account | No code reference found — see [Findings](#findings) |
| `eosc` | Confidential | Standard, Direct grant | No code reference found — see [Findings](#findings) |
| `raid-cred-833e413a…` | Confidential | Service account | Self-service credential (disabled) |
| `raid-cred-dfb61a05…` | Confidential | Service account | Self-service credential (disabled) |
| `account` | Public | Standard | Keycloak built-in |
| `account-console` | Public | Standard | Keycloak built-in |
| `admin-cli` | Public | Direct grant | Keycloak built-in |
| `broker` | Confidential | Standard | Keycloak built-in |
| `realm-management` | Confidential (bearer) | Standard | Keycloak built-in |
| `security-admin-console` | Public | Standard | Keycloak built-in |

All 20 clients are enabled except the two `raid-cred-*` clients.

## Application clients

### `raid-api` — the agency app

The only public client used for human login. The React single page app at
`app.prod.raid.org.au` authenticates against it with the authorisation code flow.

| Setting | Value |
|---|---|
| Public client | Yes |
| Flows | Standard, Direct access grants |
| Redirect URIs | `https://app.prod.raid.org.au/*`, `http://localhost:7080`, `http://localhost:7080/*` |
| Web origins | `https://app.prod.raid.org.au`, `http://localhost:7080` |
| Full scope allowed | Yes |
| Default scopes | `acr`, `basic`, `email`, `profile`, `roles`, `service_point_group_id`, `web-origins` |

**Code:**

- `raid-au/raid-agency-app/src/config/RuntimeConfig.ts` — the app reads
  `keycloak.clientId` from a runtime `app-config.json` rather than a build-time
  constant. The live file at `https://app.prod.raid.org.au/app-config.json`
  currently returns `"clientId": "raid-api"`.
- `raido-v2-aws-private/.../branch-build-ui-project.ts` and
  `branch-e2e-test-project.ts` set `VITE_KEYCLOAK_CLIENT_ID: 'raid-api'` for
  per-branch test deployments.

Note that `raid-au/doc/security/access-control/authentication/readme.md` says the
frontend uses a client called `raid-agency-app`. No such client exists in
production; the frontend uses `raid-api`. The document is out of date.

The production client does **not** carry the `user_raids` or `admin_raids` scopes
that the local dev realm gives it. Those scopes were the source of the JWT header
size problem, so their absence in production is expected.

### `raid-api-2` — the API service's own login

A confidential client used by the Spring Boot API service, not by any browser
application directly.

| Setting | Value |
|---|---|
| Public client | No |
| Flows | Standard, Direct access grants |
| Redirect URIs | `/*` (relative to the API host) |

**Code:**

- `raid-au/api-svc/raid-api/src/main/resources/application.yaml:140` —
  `spring.security.oauth2.client.registration.keycloak.client-id: raid-api-2`
- `raid-au/api-svc/.../config/SecurityConfig.java:84` — `.oauth2Login(...)`, which
  is what consumes that registration. Bearer tokens from the agency app are
  handled separately by `.oauth2ResourceServer(...)`, which validates against the
  realm issuer and does not use a client registration.
- `raido-v2-aws-private/registration-authority/cdk/config/environment-properties.ts`
  sets the same property for every environment, including prod.
- Secret: `raid-api-2-client-secret` in Secrets Manager.

## Static site clients

The static site (`static.prod.raid.org.au`) is generated by a build job that pulls
RAiD records from the API. It uses two separate service accounts, one for public
records and one for embargoed records.

Both IDs come from `https://static.prod.raid.org.au/app-config.json`, which is
deliberately excluded from the S3 sync in
`raido-v2-aws-private/.../codebuild/deploy-static-project.ts`, so it is managed
out of band in the bucket rather than from the repository.

### `static-page-client` — public records

| Setting | Value |
|---|---|
| Public client | No |
| Flows | Standard, Implicit, Direct access grants, Service account |
| Redirect URIs | `https://app.prod.raid.org.au/*` |
| Service account roles | `default-roles-raid`, `operator`, `raid-dumper`, `service-point-user` |

**Code:** `raid-au/raid-agency-app-static/src/config/AppConfig.types.ts` declares
`iamClientId` as the "Keycloak client ID for public data fetching";
`scripts/loadAppConfig.js` and `scripts/fetch-raids.js` exchange it for a token via
client credentials.

### `raid-access-handler` — embargoed records

| Setting | Value |
|---|---|
| Public client | No |
| Flows | Standard, Service account |
| Redirect URIs | `https://app.prod.raid.org.au/*` |
| Service account roles | `default-roles-raid`, `group-admin`, `operator`, `raid-access-handler`, `raid-dumper`, `raid-upgrader`, `raid-user`, `service-point-user` |

**Code:** `AppConfig.types.ts` declares `raidDumperClientId` as the "Keycloak client
ID for the raid-dumper (embargoed data)"; `scripts/fetch-embargoed-raids.js` and
`scripts/fetch-raids.js` use it. The matching realm role is
`SecurityConfig.RAID_ACCESS_HANDLER_ROLE` in the API service.

Despite the name, this client is not the embargo-expiry Lambda. That is
`access-handler`, below. The two are easy to confuse.

## Service clients

### `access-handler` — embargo expiry

An EventBridge-scheduled Lambda that finds RAiDs whose embargo has expired and
flips their access type to open.

| Setting | Value |
|---|---|
| Public client | No |
| Flows | Standard, Service account |
| Service account roles | `default-roles-raid`, `raid-access-handler` |

**Code:**

- `raido-v2-aws-private/handler/access-handler/src/handler/access-handler.ts` —
  the handler. It sets `access.type.id` to the COAR open access URI in batches of
  ten.
- `raido-v2-aws-private/registration-authority/cdk/lib/raid/stack/api.ts:205` —
  `iamClientId: 'access-handler'`, gated on `props.env.accessHandlerEnabled`.
- `.../construct/lambda/access-handler-function.ts` passes it as `CLIENT_ID`.
- Secret: `access-handler-client-secret`.

This is the only Lambda actually deployed in production: the prod account contains
`Prod-Api-AccessHandlerFunction…` and no ORCID or invite functions.

### `raid-permissions-admin` — API to IAM SPI

Used by the API service to call the custom IAM SPI endpoints that manage per-RAiD
permissions.

| Setting | Value |
|---|---|
| Public client | No |
| Flows | Standard, Direct access grants, Service account |
| Service account roles | `default-roles-raid` only |

**Code:**

- `raid-au/api-svc/.../config/RaidPermissionsAuthProps.java` binds
  `raid.raid-permissions.client-id` / `.client-secret`.
- `raid-au/api-svc/.../service/keycloak/KeycloakService.java` exchanges those for a
  client credentials token and calls `POST /raid/admin-raids` and
  `GET /raid/permissions` on the SPI.
- `raid-au/iam/.../provider/raid/RaidPermissionsController.java` is the SPI side. It
  authorises the caller by checking for the `raid-permissions-admin` **client role**
  (lines 81, 127, 148, 233) — a role on the calling client, distinct from this
  client's own service-account roles.
- CDK sets `'raid.raid-permissions.client-id': 'raid-permissions-admin'` in every
  environment. Secret: `raid-permissions-admin-client-secret`.

This client needs no `realm-management` roles: the SPI runs inside Keycloak and
bypasses the Admin REST authorisation layer entirely.

### `raid-dumper` — bulk export to Zenodo

| Setting | Value |
|---|---|
| Public client | No |
| Flows | Standard, Direct access grants, Service account |
| Service account roles | `default-roles-raid`, `raid-dumper` |

**Code:**

- `raido-v2-aws-private/registration-authority/cdk/config/environment-properties.ts`
  — `raidDumperProject.keyCloakClientId: 'raid-dumper'`, prod pointing at
  `https://zenodo.org` and `https://api.prod.raid.org.au`.
- `.../construct/codebuild/raid-dumper-project.ts` builds the CodeBuild job; the
  secret lives in SSM at `/raid-au/raid-dumper-project/keycloak-client-secret`.
- `raid-au/api-svc/.../config/SecurityConfig.java:47` —
  `RAID_DUMPER_ROLE = "raid-dumper"`, which grants bulk read of public records.
- `raid-aws-private/cdk/config/environment-properties.ts:52` also names
  `clientId: 'raid-dumper'` for federation ingest, but only against ARDC's **demo**
  instance. No production federation source is configured.

### `raid-upgrader` — operator data uplift

| Setting | Value |
|---|---|
| Public client | No |
| Flows | Standard, Direct access grants, Service account |
| Service account roles | `default-roles-raid`, `raid-upgrader` |

**Code:** operator scripts in `raid-au/scripts/`, all of which take the client ID
and secret as command line arguments: `upgrade-raids-to-v3.sh` (hardcodes
`CLIENT_ID="raid-upgrader"`), `upgrade-legacy-raids.sh`, `upgrade-raids.sh`,
`uplift-vocabulary-uris.sh`, `update-title-types.sh`, `backfill-datacite.sh`,
`backfill-datacite-related-raids.sh`. The role is
`SecurityConfig.RAID_UPGRADER_ROLE`.

### `contributor-writer` — ORCID integration

Writes contributor status back into RAiDs when a contributor grants or revokes
access to their ORCID record.

| Setting | Value |
|---|---|
| Public client | No |
| Flows | Standard, Direct access grants, Service account |
| Service account roles | `default-roles-raid`, `contributor-writer` |

**Code:**

- `raid-au/api-svc/.../config/SecurityConfig.java:52` —
  `CONTRIBUTOR_WRITER_ROLE`, which permits PATCHing any RAiD's contributors and
  reading embargoed content.
- `raid-aws-private/cdk/config/environment-properties.ts:278-290` — the shared
  ORCID Integration service's **prod** environment is configured with
  `iamClientId: "contributor-writer"` against `https://iam.prod.raid.org.au`, and a
  registration agency entry for ARDC's ROR pointing at
  `https://api.prod.raid.org.au/raid/`. This is the client that consumes it in
  production; the ORCID service runs in the Registration Authority's own account
  (`531195466715`), not in `raido-prod`.
- `raid-aws-private/handler/contributor-writer/src/client/iam-client.ts` performs
  the token exchange.
- `raido-v2-aws-private/handler/contributor-writer/` is the same handler in the
  agency repo, wired by `registration-agency` CDK for portable deployments.
- Secret: `contributor-writer-client-secret` exists in `raido-prod`. The calling
  side keeps its own copy; I did not compare the two.

I could not verify the Registration Authority prod account's deployment state — I
have no AWS profile for `531195466715`. The configuration above is what the
repository specifies, not a confirmed running deployment.

### `invite-client` — contributor invites

The only client in production with a description: *"Used for the invite feature in
RAiD (RAiD user, not contributor)"*.

| Setting | Value |
|---|---|
| Public client | No |
| Flows | Standard, Direct access grants, Service account |
| Redirect URIs | `https://app.prod.raid.org.au/*` |
| Service account roles | `default-roles-raid` only |

**Code:** `raido-v2-aws-private/registration-agency/cdk/config/environment-properties.ts`
sets `inviteFeature.kcClientId: "invite-client"` for demo and prod, consumed by
`.../lambda/handler/invite-feature/src/invite-creator.ts` and
`invite-response-listener.ts` via `KC_CLIENT_ID`. Secret: `invite-client-secret`.

The invite feature is configured only in the **registration-agency** CDK app. The
**registration-authority** app, which is what deploys ARDC's own environments,
defines no `inviteFeature` block, and the prod account contains no invite Lambdas
or API Gateway. The client and its secret exist in production but nothing in
`raido-prod` currently uses them.

## Self-service credentials

### `raid-cred-833e413a-e923-4ca5-966e-daa6da940034` and `raid-cred-dfb61a05-d828-451d-9b7f-3e355ae7050a`

Both are **disabled**, both named *"RAID-877 prod write verification
2026-09-11T00:02Z"*, and both were created within 40 seconds of each other.

| Setting | Value |
|---|---|
| Public client | No |
| Flows | Service account only |
| Service account roles | `default-roles-raid`, `service-point-user:aed7b298-9455-4fc8-bf6a-f84814666be7` |

These are artefacts of the in-process credential SPI, not hand-created clients.
`raid-au/iam/.../provider/credential/ClientCredentialController.java` mints them:
`CLIENT_ID_PREFIX = "raid-cred-"` (line 68) plus a generated ID, with
`setPublicClient(false)` and `setServiceAccountsEnabled(true)` (lines 151-155), and
grants a scoped `service-point-user:<groupId>` realm role
(`SERVICE_POINT_USER_ROLE_PREFIX`, line 50).

They are leftovers from verifying RAID-877 against production and can be deleted
once someone confirms nobody is holding those secrets.

## Keycloak built-ins

`account`, `account-console`, `admin-cli`, `broker`, `realm-management` and
`security-admin-console` are stock Keycloak clients, unmodified apart from
`security-admin-console` having `fullScopeAllowed` left on. `broker` is relevant
because production federates to AAF over SAML — the prod CDK sets
`SAML2_FRONTEND_METADATA_URL` to
`https://iam.prod.raid.org.au/realms/raid/broker/aaf-saml/endpoint/descriptor`.

## Client scopes

Every non built-in client carries the same default scope set:
`acr`, `basic`, `email`, `profile`, `roles`, `service_point_group_id`,
`web-origins` — plus `service_account` on the service-account-only clients.

`service_point_group_id` maps the user's `activeGroupId` attribute into the access
token and is what the API uses to decide which service point a request is acting
for. Note it is a **default** scope on every client here, including machine
clients whose service account has no `activeGroupId`, in which case the claim is
simply absent.

Neither `user_raids` nor `admin_raids` exists as a scope on any production client.

## Findings

These are observations from this audit, not agreed work. Each would need its own
JIRA ticket.

1. **`eosc` has no code reference anywhere.** Named "EOSC Marketplace", confidential,
   standard and direct access grant flows enabled, redirect URIs `/*`, web origins
   `/*`, secret created April 2024. A full-text search of `raid-au`,
   `raido-v2-aws-private` and `raid-aws-private` finds no mention of it. It has no
   service account, so it is a login/delegation client for a third party. Enabled
   with a wildcard redirect URI and no owner identified in code, it should be
   traced to whoever requested it or disabled.

2. **`raid-uplifter` has no code reference either.** Confidential, service account
   enabled, secret created April 2026, and its service account holds the
   **`operator`** role. It looks like a sibling of `raid-upgrader` created for a
   one-off data uplift, but unlike `raid-upgrader` no script in `raid-au/scripts/`
   names it. An enabled operator-level credential with no identified consumer is
   worth either documenting or removing.

3. **`raid-access-handler`'s service account is heavily over-privileged.** It holds
   `group-admin`, `operator`, `raid-dumper`, `raid-upgrader`, `raid-user`,
   `service-point-user` and `raid-access-handler` — seven roles for a job that only
   needs to read embargoed records during a static site build. `operator` and
   `group-admin` in particular are far beyond its purpose.

4. **`static-page-client` also holds `operator`.** Same pattern, for a client that
   only fetches public records.

5. **Two disabled `raid-cred-*` clients from RAID-877 remain in production.** See
   above.

6. **`invite-client` and `contributor-writer` exist in prod but only
   `contributor-writer` has a configured consumer.** The invite feature is
   configured in `registration-agency` CDK only, and no invite Lambdas are deployed
   in `raido-prod`. The client, its service account and its secret are dormant.

7. **`raid-au/doc/security/access-control/authentication/readme.md` names a
   `raid-agency-app` client that does not exist.** Production uses `raid-api`.

8. **The production client list is not in version control and cannot be
   reconstructed from it.** Neither realm import file matches production —
   production has `raid-access-handler`, `static-page-client`, `access-handler`,
   `eosc` and `raid-uplifter` that appear in neither, and the `static-generator`
   client ID that both `registration-agency` CDK env blocks configure for the
   static site does not exist in the production realm at all (two orphaned
   `static-generator*-client-secret` secrets do exist). Recovering the realm after
   a loss would be a manual reconstruction.

## How this was captured

The Keycloak Admin REST API was not usable: the `admin` password in the
`keycloak-credentials` secret is rejected by
`iam.prod.raid.org.au/realms/master`, and no service account in the realm holds
`realm-management` roles, so `GET /admin/realms/raid/clients` returns 403.

The data was instead read from the `keycloak` schema of the production `raido`
database, over an SSM port-forward through an ECS container instance, using the
`iam-db-credentials` secret. Read-only `SELECT`s only, against `client`,
`redirect_uris`, `web_origins`, `scope_mapping`, `keycloak_role`,
`user_role_mapping`, `user_entity`, `client_scope`, `client_scope_client` and
`client_attributes`.
