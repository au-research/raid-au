# RAID-1037 probe: correcting a RAiD minted with the wrong landing prefix

Checks which API calls correct a RAiD whose `identifier.raidAgencyUrl` and
DataCite URL were built from a wrong `raid.identifier.landing-prefix`.

Needs the local dev stack, `jq`, and the API run twice.

```
./gradlew :api-svc:raid-api:dockerComposeUp

# Phase 1: mint with the wrong prefix
_JAVA_OPTIONS="-Draid.identifier.landing-prefix=https://static.api_user.raid.org.au/raids/ ..." \
  ./gradlew :api-svc:raid-api:bootRun -Dspring.profiles.active=dev
./verify.sh mint                 # prints the handle

# Phase 2: restart with the correct prefix, then fix
_JAVA_OPTIONS="-Draid.identifier.landing-prefix=https://static.raid.surf.nl/raids/ ..." \
  ./gradlew :api-svc:raid-api:bootRun -Dspring.profiles.active=dev
./verify.sh fix <handle>

./gradlew :api-svc:raid-api:dockerComposeDown
```

`...` stands for the usual local settings (`raid.db.encryption-key` from
`application-dev.yaml`, `raid.db.user`, `raid.db.password`).

`fix` tries an unchanged PUT, then a PUT with only `raidAgencyUrl` corrected,
and after each prints the `raidAgencyUrl` in every `raid_history` revision
and the `url` of every DataCite call MockServer recorded for the handle.

Result on 6 November 2026 (`origin/main` at `be3b6ece`): the unchanged PUT
saves nothing and calls nothing; the corrected PUT saves version 2 with the
new URL, updates `raid.metadata`, and sends DataCite the new URL.

See `documentation/20261106-raid-1037-surf-landing-url.md`.
