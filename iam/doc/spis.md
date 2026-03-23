# Keycloak SPIs

The IAM module provides two custom `RealmResourceProvider` SPIs that expose REST endpoints within Keycloak.

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

## RAiD Permissions Controller

Manages per-RAiD access control, allowing users to be granted user or admin permissions on individual RAiDs.

| Method | Path            | Description                                        |
|--------|-----------------|----------------------------------------------------|
| POST   | `/raid-user`    | Grant raid-user permission to a user on a RAiD     |
| DELETE | `/raid-user`    | Revoke raid-user permission from a user on a RAiD  |
| POST   | `/raid-admin`   | Grant raid-admin permission to a user on a RAiD    |
| DELETE | `/raid-admin`   | Revoke raid-admin permission from a user on a RAiD |
| POST   | `/admin-raids`  | List RAiDs a user has admin permissions on         |
