# Local Development

When running locally with Docker Compose, the `raid` realm should be added automatically when the container starts.

## Test Users

The realm includes two test users, each belonging to different service points. Both have the `service-point-user` role necessary to mint RAiDs and the password `password`.

| User             | role                | Password   |
|------------------|---------------------|------------|
| `raid-test-user` | `service-point-user`  | `password` |
| `raid-operator`  | `operator` | `password` |

## Exporting Realm Changes

If you make any changes to the realm and want to save them to the configuration, run:

```bash
docker exec -it [container-id] bin/kc.sh export --realm raid --file /opt/keycloak/data/import/local-raid-realm.json
```

Then commit the changes.

See the [Keycloak Import/Export documentation](https://www.keycloak.org/server/importExport) for more details.
