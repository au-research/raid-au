# RAID-1037 probe: changing raid.environment on a populated database

Shows what happens when an agency that created its database with
`raid.environment=api_user` later switches to `raid.environment=prod`.

```
./probe.sh
```

Needs Docker only. It starts a throwaway Postgres 16 container and runs the
Flyway CLI at the same version as `flyway-core` (11.20.3), with the same
schema, baseline and location settings as `application.yaml`. Everything is
removed on exit.

Steps:

1. Migrate with `raid.environment=api_user`, and check whether
   `V2__create_api_user` ran and whether the `api_user` role exists.
2. Seed one `raid_history` row carrying the `api_user` landing URL.
3. Re-run Flyway with the `prod` locations.
4. Re-run with `outOfOrder=true` and show what happened to the seeded row.

Result on 6 November 2026 (`origin/main` at `be3b6ece`): step 1 applies no
V2 and creates no role; step 3 fails validation with "Detected resolved
migration not applied" for 34.1 to 41.1; step 4 succeeds but `V36.1`
rewrites the row's `raidAgencyUrl` to `https://static.prod.raid.org.au/...`.

See `documentation/20261106-raid-1037-surf-landing-url.md`.
