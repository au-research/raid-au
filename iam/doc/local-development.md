# Local Development

When running locally with Docker Compose, the `raid` realm should be added automatically when the container starts.

## Test Users

The realm includes several test users. All have the password `password`.

| User                        | role                              | Notes                                                                    |
|-----------------------------|------------------------------------|---------------------------------------------------------------------------|
| `raid-test-user`            | `service-point-user`              | Approved member of `raid-au`                                             |
| `raid-operator`             | `operator`                        | No group membership                                                      |
| `raid-au-group-admin`       | `group-admin`, `service-point-user` | Approved admin of `raid-au`                                            |
| `raid-au-pending-user`      | (none)                             | Raw, self-joined member of `raid-au` with an outstanding access request  |
| `raid-au-unapproved-admin`  | `group-admin`                     | Raw, self-joined member of `raid-au`, never approved for it (RAID-608)    |

## Exporting Realm Changes

If you make any changes to the realm and want to save them to the configuration, run:

```bash
docker exec -it [container-id] bin/kc.sh export --realm raid --file /opt/keycloak/data/import/local-raid-realm.json
```

Then commit the changes.

See the [Keycloak Import/Export documentation](https://www.keycloak.org/server/importExport) for more details.
