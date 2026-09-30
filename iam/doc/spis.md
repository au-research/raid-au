# Keycloak SPIs

The IAM module provides custom `RealmResourceProvider` SPIs that expose REST endpoints within Keycloak. Each is served under `/realms/raid/<provider id>`.

## Group Controller

Manages service point groups — the organisational units that users belong to in order to mint and manage RAiDs.

| Method | Path             | Description                                  |
|--------|------------------|----------------------------------------------|
| GET    | `/all`           | List all groups (operator only)              |
| GET    | `/`              | Get the current user's group                 |
| PUT    | `/grant`         | Grant a role to a user within a group        |
| PUT    | `/revoke`        | Revoke a role from a user within a group     |
| PUT    | `/group-admin`   | Add a group admin                            |
| DELETE | `/group-admin`   | Remove a group admin                         |
| PUT    | `/join`          | Join a group                                 |
| PUT    | `/leave`         | Leave a group                                |
| PUT    | `/active-group`  | Set the active group for the current user    |
| DELETE | `/active-group`  | Remove the active group for the current user |
| GET    | `/user-groups`   | List groups for the current user             |
| POST   | `/create`        | Create a new group                           |
| DELETE | `/delete`        | Delete a group                               |
| POST   | `/migrate-service-point-admins` | Backfill scoped `service-point-admin:<groupId>` roles for flat group-admin holders (operator only, idempotent) |

## RAiD Permissions Controller

Manages per-RAiD access control, allowing users to be granted user or admin permissions on individual RAiDs.

| Method | Path            | Description                                        |
|--------|-----------------|----------------------------------------------------|
| POST   | `/raid-user`    | Grant raid-user permission to a user on a RAiD     |
| DELETE | `/raid-user`    | Revoke raid-user permission from a user on a RAiD  |
| POST   | `/raid-admin`   | Grant raid-admin permission to a user on a RAiD    |
| DELETE | `/raid-admin`   | Revoke raid-admin permission from a user on a RAiD |
| POST   | `/admin-raids`  | List RAiDs a user has admin permissions on         |

## Client Credential Controller

Lets a Service Point Admin self-serve their service point's API client credentials (RAID-827). Served under `/realms/raid/client-credential`. Each credential is a confidential Keycloak client (`raid-cred-<uuid>`) whose service account holds only the scoped `service-point-user:<groupId>` role.

Every endpoint requires the `operator` role or the scoped `service-point-admin:<groupId>` role for the credential's service point. The flat legacy `group-admin` role is rejected. Clients this SPI did not create are treated as not found, so it can never read or change the realm's own clients. A service point may hold at most 10 active credentials.

| Method | Path       | Description                                                                          |
|--------|------------|--------------------------------------------------------------------------------------|
| POST   | `/`        | Create a credential for a service point (`groupId`, `label`); returns the secret     |
| GET    | `/`        | List a service point's credentials, active and revoked, without secrets (`groupId`)  |
| GET    | `/secret`  | Return a credential's current secret (`clientId`)                                    |
| POST   | `/rotate`  | Issue a new secret; the old one stops working immediately (`clientId`)               |
| DELETE | `/`        | Revoke a credential by disabling it; idempotent (`clientId`)                         |
| DELETE | `/delete`  | Permanently delete a credential and its service account (`clientId`) (RAID-921)      |

Responses that carry a secret (create, get-secret, rotate) are sent with `Cache-Control: no-store`. Successful actions are audit-logged to the `au.org.raid.iam.audit` logger without secret values. For worked `curl` examples, see [`doc/reference/service-point-client-credentials.md`](../../doc/reference/service-point-client-credentials.md).
