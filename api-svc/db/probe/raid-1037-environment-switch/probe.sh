#!/usr/bin/env bash
# RAID-1037 probe: what happens when an agency changes raid.environment on a
# database that already has data in it.
#
# Reproduces the presumed SURF state (raid.environment=api_user), seeds one
# RAiD history row carrying the wrong raidAgencyUrl, then re-runs Flyway with
# the locations raid.environment=prod would give. Runs entirely in throwaway
# containers; nothing outside Docker is touched.
#
# Usage: ./probe.sh            (needs Docker; run from anywhere)
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
DB_RES="$(cd "$HERE/../../src/main/resources/db" && pwd)"
FLYWAY_IMAGE="flyway/flyway:11.20.3"   # matches flyway-core in gradle/libs.versions.toml
PG_IMAGE="postgres:16"
NET="raid1037-probe"
PG="raid1037-probe-pg"

cleanup() {
  docker rm -f "$PG" >/dev/null 2>&1 || true
  docker network rm "$NET" >/dev/null 2>&1 || true
}
trap cleanup EXIT
cleanup

docker network create "$NET" >/dev/null
docker run -d --name "$PG" --network "$NET" \
  -e POSTGRES_PASSWORD=probe -e POSTGRES_DB=raido "$PG_IMAGE" >/dev/null
until docker exec "$PG" pg_isready -U postgres -d raido >/dev/null 2>&1; do sleep 1; done
sleep 2

# Mirrors spring.flyway.* in application.yaml plus the placeholders the API and
# the db Gradle build supply. SURF's ROR maps to block 3 (start 30000000).
flyway() {
  local env="$1"; shift
  docker run --rm --network "$NET" \
    -v "$DB_RES:/db:ro" \
    "$FLYWAY_IMAGE" \
    -url="jdbc:postgresql://$PG:5432/raido" -user=postgres -password=probe \
    -defaultSchema=api_svc -schemas=api_svc -createSchemas=true \
    -baselineVersion=25 \
    -locations="filesystem:/db/env/api_user,filesystem:/db/migration,filesystem:/db/env/$env" \
    -placeholders.apiSvcRolePassword=probe \
    -placeholders.servicePointIdStart=30000000 \
    -placeholders.servicePointIdBlockSize=10000000 \
    "$@"
}

psql() { docker exec -i "$PG" psql -U postgres -d raido -v ON_ERROR_STOP=1 -qtA "$@"; }

echo "== 1. Create the database as raid.environment=api_user (presumed SURF state)"
flyway api_user migrate 2>&1 | grep -E 'Successfully|ERROR|WARN' || true
echo "applied: $(psql -c "select count(*) from api_svc.flyway_schema_history where success")" \
     "latest: $(psql -c "select max(version) from api_svc.flyway_schema_history where success and version is not null and type <> 'BASELINE'")"

echo "V2__create_api_user applied: $(psql -c "select count(*) from api_svc.flyway_schema_history where version = '2'") row(s);" \
     "api_user role exists: $(psql -c "select count(*) from pg_roles where rolname = 'api_user'")"

echo
echo "== 2. Seed one raid_history row with the api_user raidAgencyUrl"
psql <<'SQL'
insert into api_svc.raid_history (handle, revision, change_type, diff, created)
values ('10.83338/831b5565', 1, 'PATCH',
        '[{"op":"add","path":"/identifier","value":{"id":"https://raid.org/10.83338/831b5565","raidAgencyUrl":"https://static.api_user.raid.org.au/raids/10.83338/831b5565"}}]',
        now());
SQL
psql -c "select diff::jsonb->0->'value'->>'raidAgencyUrl' from api_svc.raid_history"

echo
echo "== 3. Switch to raid.environment=prod and start (Flyway validate + migrate)"
if flyway prod migrate > "$HERE/.out-switch.log" 2>&1; then
  echo "RESULT: migrate SUCCEEDED"
else
  echo "RESULT: migrate FAILED"
fi
grep -E 'ERROR|Detected|Validate failed|not applied' "$HERE/.out-switch.log" | head -12 || true

echo
echo "== 4. Same switch with outOfOrder=true (what someone might try next)"
if flyway prod migrate -outOfOrder=true > "$HERE/.out-ooo.log" 2>&1; then
  echo "RESULT: migrate SUCCEEDED"
else
  echo "RESULT: migrate FAILED"
fi
grep -E 'Migrating|Successfully|ERROR' "$HERE/.out-ooo.log" | head -12 || true
echo "raidAgencyUrl now: $(psql -c "select diff::jsonb->0->'value'->>'raidAgencyUrl' from api_svc.raid_history")"
