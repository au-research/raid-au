# IAM — Keycloak SPIs

This module contains custom Keycloak Service Provider Interfaces (SPIs) and configuration for the RAiD identity and access management layer.

## Local Development

When running locally with Docker Compose, the `raid` realm should be added automatically when the container starts.

## Test Users

The realm includes two test users, each belonging to different service points. Both have the `service-point-user` role necessary to mint RAiDs and the password `password`.

| User             | Password   |
|------------------|------------|
| `raid-test-user` | `password` |
| `uq-test-user`   | `password` |

## Exporting Realm Changes

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