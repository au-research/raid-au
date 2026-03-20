# IAM — Keycloak SPIs

This module contains custom Keycloak Service Provider Interfaces (SPIs) and configuration for the RAiD identity and access management layer.

## SPIs

The module provides two custom `RealmResourceProvider` SPIs that expose REST endpoints within Keycloak:

### Group Controller

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

### RAiD Permissions Controller

Manages per-RAiD access control, allowing users to be granted user or admin permissions on individual RAiDs.

| Method | Path            | Description                                        |
|--------|-----------------|----------------------------------------------------|
| POST   | `/raid-user`    | Grant raid-user permission to a user on a RAiD     |
| DELETE | `/raid-user`    | Revoke raid-user permission from a user on a RAiD  |
| POST   | `/raid-admin`   | Grant raid-admin permission to a user on a RAiD    |
| DELETE | `/raid-admin`   | Revoke raid-admin permission from a user on a RAiD |
| POST   | `/admin-raids`  | List RAiDs a user has admin permissions on         |

## Local Development

When running locally with Docker Compose, the `raid` realm should be added automatically when the container starts.

### Test Users

The realm includes two test users, each belonging to different service points. Both have the `service-point-user` role necessary to mint RAiDs and the password `password`.

| User             | role                | Password   |
|------------------|---------------------|------------|
| `raid-test-user` | `service-point-user`  | `password` |
| `raid-operator`  | `operator` | `password` |

### Exporting Realm Changes

If you make any changes to the realm and want to save them to the configuration, run:

```bash
docker exec -it [container-id] bin/kc.sh export --realm raid --file /opt/keycloak/data/import/local-raid-realm.json
```

Then commit the changes.

See the [Keycloak Import/Export documentation](https://www.keycloak.org/server/importExport) for more details.

## Related Documentation

- [Authorization Code Flow](doc/authorization-code-flow.md)
- [Authorization Code Flow — Client](doc/authorization-code-flow-client.md)
- [Authorization Code Flow — Public Client](doc/authorization-code-flow-public-client.md)
- [Client Credentials Flow](doc/client-credentials-flow.md)
- [Tokens](doc/tokens.md)
- [Role Permissions](doc/role-permissions.md)
- [Service Point Group ID](doc/service-point-group-id.md)
- [activeGroupId to Service Point Group ID Mapping](doc/activeGroupId-to-service-point-group-id-mapping.md)