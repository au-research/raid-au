#!/usr/bin/env bash
# RAID-1037: verify how a RAiD minted with a wrong raid.identifier.landing-prefix
# can be corrected through the API alone.
#
# Run against the local dev stack (./gradlew :api-svc:raid-api:dockerComposeUp)
# in two phases, restarting the API between them:
#
#   1. API started with -Draid.identifier.landing-prefix=https://static.api_user.raid.org.au/raids/
#        ./verify.sh mint            -> mints a RAiD, prints its handle
#   2. API restarted with -Draid.identifier.landing-prefix=https://static.raid.surf.nl/raids/
#        ./verify.sh fix <handle>    -> unchanged PUT, then PUT with corrected raidAgencyUrl
#
# DataCite calls go to the local MockServer (localhost:1080); the script reads
# the URL each PUT /dois call carried from MockServer's request log.
set -euo pipefail

API=http://localhost:8080
KC=http://localhost:8001/realms/raid/protocol/openid-connect/token
MOCK=http://localhost:1080
GOOD_PREFIX=https://static.raid.surf.nl/raids/
HERE="$(cd "$(dirname "$0")" && pwd)"
FIXTURE="$HERE/../../src/test/resources/fixtures/create-raid.json"

token() {
  curl -sf "$KC" -d client_id=raid-api -d grant_type=password \
    -d username=raid-test-user -d password=password | jq -r .access_token
}

datacite_urls() {
  # Every DataCite call for this handle, oldest first (mint is POST /dois,
  # updates are PUT /dois/<handle>): method, path and the "url" attribute sent.
  curl -sf -X PUT "$MOCK/mockserver/retrieve?type=REQUESTS&format=JSON" -d '{"path":"/dois.*"}' |
    jq -r --arg h "$1" '.[] | {m: .method, p: .path, u: ((.body.json // (.body.string | fromjson? // {})) | .data.attributes.url // "")}
                        | select(.u | endswith($h)) | "  \(.m) \(.p)  url=\(.u)"'
}

history() {
  docker exec raid-api-raid-api-db-1 psql -U postgres -d raido -qtA -c \
    "select h.revision, h.change_type, coalesce(
            (select case e->>'path' when '/identifier' then e->'value'->>'raidAgencyUrl' else e->>'value' end from jsonb_array_elements(h.diff::jsonb) e
              where e->>'path' in ('/identifier/raidAgencyUrl', '/identifier') limit 1),
            '(raidAgencyUrl unchanged)')
       from api_svc.raid_history h where h.handle = '$1' order by h.revision, h.change_type"
}

case "${1:-}" in
  mint)
    TOKEN=$(token)
    # Service point comes from the token. Point the contributor and organisation
    # at an ORCID and ROR the dev MockServer knows (docker-compose/mockserver).
    BODY=$(jq '.contributor[0] += {id: "https://sandbox.orcid.org/0009-0002-5128-5184",
                                   schemaUri: "https://sandbox.orcid.org/", leader: true, contact: true}
               | .organisation[0].id = "https://ror.org/038sjwq14"' "$FIXTURE")
    RESP=$(curl -s -w '\n%{http_code}' -X POST "$API/raid/" -H "Authorization: Bearer $TOKEN" \
      -H 'Content-Type: application/json' -d "$BODY")
    CODE=$(tail -1 <<<"$RESP"); JSON=$(sed '$d' <<<"$RESP")
    echo "mint HTTP $CODE"
    [[ "$CODE" == 201 ]] || { echo "$JSON" | head -c 2000; exit 1; }
    HANDLE=$(jq -r '.identifier.id | sub("^https?://[^/]+/"; "")' <<<"$JSON")
    echo "handle: $HANDLE"
    echo "raidAgencyUrl: $(jq -r .identifier.raidAgencyUrl <<<"$JSON")"
    echo "DataCite url(s) sent:"; datacite_urls "$HANDLE"
    ;;
  fix)
    HANDLE="${2:?handle required}"
    TOKEN=$(token)
    CURRENT=$(curl -sf -H "Authorization: Bearer $TOKEN" "$API/raid/$HANDLE")
    echo "GET raidAgencyUrl (after config fix): $(jq -r .identifier.raidAgencyUrl <<<"$CURRENT")"
    echo "GET version: $(jq -r .identifier.version <<<"$CURRENT")"

    echo; echo "-- A. PUT the record back unchanged (what an ordinary save sends)"
    curl -s -o /dev/null -w 'HTTP %{http_code}\n' -X PUT "$API/raid/$HANDLE" \
      -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$CURRENT"
    history "$HANDLE"; echo "DataCite url(s) sent so far:"; datacite_urls "$HANDLE"

    echo; echo "-- B. PUT with identifier.raidAgencyUrl corrected"
    FIXED=$(jq --arg u "$GOOD_PREFIX$HANDLE" '.identifier.raidAgencyUrl = $u' <<<"$CURRENT")
    RESP=$(curl -s -w '\n%{http_code}' -X PUT "$API/raid/$HANDLE" \
      -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$FIXED")
    echo "HTTP $(tail -1 <<<"$RESP")"
    echo "response raidAgencyUrl: $(sed '$d' <<<"$RESP" | jq -r .identifier.raidAgencyUrl)"
    history "$HANDLE"; echo "DataCite url(s) sent so far:"; datacite_urls "$HANDLE"

    echo; echo "-- C. Read back"
    AFTER=$(curl -sf -H "Authorization: Bearer $TOKEN" "$API/raid/$HANDLE")
    echo "GET raidAgencyUrl: $(jq -r .identifier.raidAgencyUrl <<<"$AFTER")  version: $(jq -r .identifier.version <<<"$AFTER")"
    # /raid/all-public (the static site's source) reads this column; it needs
    # the raid-dumper role, so the column is read directly here.
    echo "materialised metadata raidAgencyUrl: $(docker exec raid-api-raid-api-db-1 psql -U postgres -d raido -qtA -c \
      "select metadata::jsonb->'identifier'->>'raidAgencyUrl' from api_svc.raid where handle = '$HANDLE'")"
    ;;
  *) echo "usage: $0 mint | fix <handle>"; exit 2 ;;
esac
