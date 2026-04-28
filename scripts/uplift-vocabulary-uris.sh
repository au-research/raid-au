#!/bin/bash

# Uplifts legacy raids that use github.com vocabulary URIs to use modern
# vocabulary.raid.org / COAR equivalents. Reads each raid via the API,
# replaces URIs, and PUTs it back to create a new version.
#
# Usage: ./scripts/uplift-vocabulary-uris.sh <environment> <client_id> <client_secret>
#
# Prerequisites: curl, jq

set -euo pipefail

DRY_RUN=false
if [ "$1" = "--dry-run" ]; then
    DRY_RUN=true
    shift
fi

if [ "$#" -ne 3 ]; then
    echo "Usage: $0 [--dry-run] <environment> <client_id> <client_secret>"
    echo "Environment must be one of: local, test, demo, stage, prod"
    echo "Example: $0 --dry-run test raid-upgrader uQdAmNCYOPeEL58sNBiwBqeyKC4evv71"
    exit 1
fi

ENVIRONMENT="$1"
CLIENT_ID="$2"
CLIENT_SECRET="$3"

case "$ENVIRONMENT" in
    local)
        API_BASE_URL="http://localhost:8080"
        IAM_BASE_URL="http://localhost:8001"
        ;;
    test)
        API_BASE_URL="https://api.test.raid.org.au"
        IAM_BASE_URL="https://iam.test.raid.org.au"
        ;;
    demo)
        API_BASE_URL="https://api.demo.raid.org.au"
        IAM_BASE_URL="https://iam.demo.raid.org.au"
        ;;
    stage)
        API_BASE_URL="https://api.stage.raid.org.au"
        IAM_BASE_URL="https://iam.stage.raid.org.au"
        ;;
    prod)
        API_BASE_URL="https://api.prod.raid.org.au"
        IAM_BASE_URL="https://iam.prod.raid.org.au"
        ;;
    *)
        echo "Error: Invalid environment '$ENVIRONMENT'"
        exit 1
        ;;
esac

TOKEN_URL="${IAM_BASE_URL}/realms/raid/protocol/openid-connect/token"

echo "Environment: $ENVIRONMENT"
echo "API: $API_BASE_URL"
echo "IAM: $IAM_BASE_URL"
if [ "$DRY_RUN" = true ]; then
    echo "MODE: DRY RUN (no changes will be made)"
fi
echo ""

# --- Authentication ---
get_token() {
    local response
    response=$(curl -s -X POST "$TOKEN_URL" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials" \
        -d "client_id=$CLIENT_ID" \
        -d "client_secret=$CLIENT_SECRET")

    echo "$response" | jq -r '.access_token'
}

ACCESS_TOKEN=$(get_token)
if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
    echo "ERROR: Failed to obtain access token"
    exit 1
fi
echo "Authenticated successfully"

# --- Build jq replacement filter ---
# This jq filter replaces all legacy github.com vocabulary URIs with modern equivalents
# and transforms leader/contact contributor positions into boolean flags.
JQ_FILTER='
# Access type
def replace_access_type_id:
  if . == "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/closed.json" then "https://vocabularies.coar-repositories.org/access_rights/c_f1cf/"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/embargoed.json" then "https://vocabularies.coar-repositories.org/access_rights/c_f1cf/"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/access/type/v1/open.json" then "https://vocabularies.coar-repositories.org/access_rights/c_abf2/"
  else . end;

def replace_access_type_schema:
  if . == "https://github.com/au-research/raid-metadata/tree/main/scheme/access/type/v1/" then "https://vocabularies.coar-repositories.org/access_rights/"
  else . end;

# Title type
def replace_title_type_id:
  if . == "https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/alternative.json" then "https://vocabulary.raid.org/title.type.schema/4"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/title/type/v1/primary.json" then "https://vocabulary.raid.org/title.type.schema/5"
  else . end;

def replace_title_type_schema:
  if . == "https://github.com/au-research/raid-metadata/tree/main/scheme/title/type/v1/" then "https://vocabulary.raid.org/title.type.schema/376"
  else . end;

# Description type
def replace_description_type_id:
  if . == "https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/alternative.json" then "https://vocabulary.raid.org/description.type.schema/319"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/description/type/v1/primary.json" then "https://vocabulary.raid.org/description.type.schema/318"
  else . end;

def replace_description_type_schema:
  if . == "https://github.com/au-research/raid-metadata/tree/main/scheme/description/type/v1/" then "https://vocabulary.raid.org/description.type.schema/320"
  else . end;

# Contributor position
def replace_contributor_position_id:
  if . == "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/co-investigator.json" then "https://vocabulary.raid.org/contributor.position.schema/308"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/other-participant.json" then "https://vocabulary.raid.org/contributor.position.schema/311"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/principal-investigator.json" then "https://vocabulary.raid.org/contributor.position.schema/307"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/leader.json" then "https://vocabulary.raid.org/contributor.position.schema/307"
  else . end;

def replace_contributor_position_schema:
  if . == "https://github.com/au-research/raid-metadata/tree/main/scheme/contributor/position/v1/" then "https://vocabulary.raid.org/contributor.position.schema/305"
  else . end;

# Organisation role
def replace_organisation_role_id:
  if . == "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/contractor.json" then "https://vocabulary.raid.org/organisation.role.schema/185"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/lead-research-organisation.json" then "https://vocabulary.raid.org/organisation.role.schema/182"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/other-organisation.json" then "https://vocabulary.raid.org/organisation.role.schema/188"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/other-research-organisation.json" then "https://vocabulary.raid.org/organisation.role.schema/183"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/organisation/role/v1/partner-organisation.json" then "https://vocabulary.raid.org/organisation.role.schema/184"
  else . end;

def replace_organisation_role_schema:
  if . == "https://github.com/au-research/raid-metadata/tree/main/scheme/organisation/role/v1/" then "https://vocabulary.raid.org/organisation.role.schema/359"
  else . end;

# Related object type
def replace_related_object_type_id:
  if . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/audiovisual.json" then "https://vocabulary.raid.org/relatedObject.type.schema/273"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/book-chapter.json" then "https://vocabulary.raid.org/relatedObject.type.schema/271"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/book.json" then "https://vocabulary.raid.org/relatedObject.type.schema/258"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/computational-notebook.json" then "https://vocabulary.raid.org/relatedObject.type.schema/256"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/conference-paper.json" then "https://vocabulary.raid.org/relatedObject.type.schema/264"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/conference-poster.json" then "https://vocabulary.raid.org/relatedObject.type.schema/248"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/conference-proceeding.json" then "https://vocabulary.raid.org/relatedObject.type.schema/262"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/data-paper.json" then "https://vocabulary.raid.org/relatedObject.type.schema/255"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/dataset.json" then "https://vocabulary.raid.org/relatedObject.type.schema/269"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/dissertation.json" then "https://vocabulary.raid.org/relatedObject.type.schema/253"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/educational-material.json" then "https://vocabulary.raid.org/relatedObject.type.schema/267"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/event.json" then "https://vocabulary.raid.org/relatedObject.type.schema/260"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/funding.json" then "https://vocabulary.raid.org/relatedObject.type.schema/272"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/image.json" then "https://vocabulary.raid.org/relatedObject.type.schema/257"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/instrument.json" then "https://vocabulary.raid.org/relatedObject.type.schema/266"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/journal-article.json" then "https://vocabulary.raid.org/relatedObject.type.schema/250"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/model.json" then "https://vocabulary.raid.org/relatedObject.type.schema/263"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/output-management-plan.json" then "https://vocabulary.raid.org/relatedObject.type.schema/247"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/physical-object.json" then "https://vocabulary.raid.org/relatedObject.type.schema/270"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/preprint.json" then "https://vocabulary.raid.org/relatedObject.type.schema/254"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/prize.json" then "https://vocabulary.raid.org/relatedObject.type.schema/268"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/report.json" then "https://vocabulary.raid.org/relatedObject.type.schema/252"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/service.json" then "https://vocabulary.raid.org/relatedObject.type.schema/274"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/software.json" then "https://vocabulary.raid.org/relatedObject.type.schema/259"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/sound.json" then "https://vocabulary.raid.org/relatedObject.type.schema/261"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/standard.json" then "https://vocabulary.raid.org/relatedObject.type.schema/251"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/text.json" then "https://vocabulary.raid.org/relatedObject.type.schema/265"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/type/v1/workflow.json" then "https://vocabulary.raid.org/relatedObject.type.schema/249"
  else . end;

def replace_related_object_type_schema:
  if . == "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/type/v1/" then "https://vocabulary.raid.org/relatedObject.type.schema/329"
  else . end;

# Related object category
def replace_related_object_category_id:
  if . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/input.json" then "https://vocabulary.raid.org/relatedObject.category.id/191"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/internal.json" then "https://vocabulary.raid.org/relatedObject.category.id/192"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-object/category/v1/output.json" then "https://vocabulary.raid.org/relatedObject.category.id/190"
  else . end;

def replace_related_object_category_schema:
  if . == "https://github.com/au-research/raid-metadata/tree/main/scheme/related-object/category/v1/" then "https://vocabulary.raid.org/relatedObject.category.schemaUri/386"
  else . end;

# Related raid type
def replace_related_raid_type_id:
  if . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/has-part.json" then "https://vocabulary.raid.org/relatedRaid.type.schema/201"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/continues.json" then "https://vocabulary.raid.org/relatedRaid.type.schema/204"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-continued-by.json" then "https://vocabulary.raid.org/relatedRaid.type.schema/203"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-derived-from.json" then "https://vocabulary.raid.org/relatedRaid.type.schema/200"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-identical-to.json" then "https://vocabulary.raid.org/relatedRaid.type.schema/204"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-obsoleted-by.json" then "https://vocabulary.raid.org/relatedRaid.type.schema/205"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-part-of.json" then "https://vocabulary.raid.org/relatedRaid.type.schema/202"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/is-source-of.json" then "https://vocabulary.raid.org/relatedRaid.type.schema/199"
  elif . == "https://github.com/au-research/raid-metadata/blob/main/scheme/related-raid/type/v1/obsoletes.json" then "https://vocabulary.raid.org/relatedRaid.type.schema/198"
  else . end;

def replace_related_raid_type_schema:
  if . == "https://github.com/au-research/raid-metadata/tree/main/scheme/related-raid/type/v1/" then "https://vocabulary.raid.org/relatedRaid.type.schema/367"
  else . end;

# Leader/contact position constants
def is_leader: . == "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/leader.json";
def is_contact: . == "https://github.com/au-research/raid-metadata/blob/main/scheme/contributor/position/v1/contact-person.json";
def default_other_position($pos):
  { id: "https://vocabulary.raid.org/contributor.position.schema/311",
    schemaUri: "https://vocabulary.raid.org/contributor.position.schema/305",
    startDate: $pos.startDate
  } + (if $pos.endDate then { endDate: $pos.endDate } else {} end);

# --- Apply all replacements ---

# Access
(if .access.type.id then .access.type.id |= replace_access_type_id else . end)
| (if .access.type.schemaUri then .access.type.schemaUri |= replace_access_type_schema else . end)

# Titles
| (if .title then .title |= map(
    (if .type.id then .type.id |= replace_title_type_id else . end)
    | (if .type.schemaUri then .type.schemaUri |= replace_title_type_schema else . end)
  ) else . end)

# Descriptions
| (if .description then .description |= map(
    (if .type.id then .type.id |= replace_description_type_id else . end)
    | (if .type.schemaUri then .type.schemaUri |= replace_description_type_schema else . end)
  ) else . end)

# Contributors - handle leader/contact positions
| (if .contributor then .contributor |= map(
    # Set leader flag if any position is leader
    .leader = (if .position then (.position | any(.id | is_leader)) else false end)
    # Set contact flag if any position is contact
    | .contact = (if .position then (.position | any(.id | is_contact)) else false end)
    # Filter out leader/contact positions, replace remaining
    | (if .position then
        (.position | map(select(.id | is_contact | not))) as $remaining
        | if ($remaining | length) == 0 then
            # All positions were leader/contact - add default Other
            .position = [default_other_position(.position[0])]
          else
            .position = ($remaining | map(
              (if .id then .id |= replace_contributor_position_id else . end)
              | (if .schemaUri then .schemaUri |= replace_contributor_position_schema else . end)
            ))
          end
      else . end)
  )
  # If there is only one contributor, they must be both leader and contact
  | if (.contributor | length) == 1 then .contributor[0].leader = true | .contributor[0].contact = true
    else
      # Ensure at least one contributor is flagged as leader
      if (.contributor | any(.leader == true)) then . else .contributor[0].leader = true end
      # Ensure at least one contributor is flagged as contact
      | if (.contributor | any(.contact == true)) then . else .contributor[0].contact = true end
    end
  else . end)

# Access statement - ensure text is set
| (if .access.statement then
    if (.access.statement.text == null or .access.statement.text == "") then
      .access.statement.text = "This RAiD is open access."
    else . end
  else
    .access.statement = { text: "This RAiD is open access.", language: { id: "eng", schemaUri: "https://iso639-3.sil.org" } }
  end)

# Organisations
| (if .organisation then .organisation |= map(
    (if .role then .role |= map(
      (if .id then .id |= replace_organisation_role_id else . end)
      | (if .schemaUri then .schemaUri |= replace_organisation_role_schema else . end)
    ) else . end)
  ) else . end)

# Related objects
| (if .relatedObject then .relatedObject |= map(
    (if .type.id then .type.id |= replace_related_object_type_id else . end)
    | (if .type.schemaUri then .type.schemaUri |= replace_related_object_type_schema else . end)
    | (if .category then .category |= map(
        (if .id then .id |= replace_related_object_category_id else . end)
        | (if .schemaUri then .schemaUri |= replace_related_object_category_schema else . end)
      ) else . end)
  ) else . end)

# Related raids
| (if .relatedRaid then .relatedRaid |= map(
    (if .type.id then .type.id |= replace_related_raid_type_id else . end)
    | (if .type.schemaUri then .type.schemaUri |= replace_related_raid_type_schema else . end)
  ) else . end)
'

# --- Process raids ---

# Get all raids for the service point that may need uplift
# The /upgrade endpoint returns raids from service point 20000003 (legacy)
# We use the raid list endpoint instead to get all raids
echo "Fetching raid list..."

HANDLES_FILE=$(mktemp)
trap 'rm -f "$HANDLES_FILE"' EXIT

# Get all raids - paginate through the list
PAGE=1
PAGE_SIZE=100
TOTAL_PROCESSED=0
TOTAL_UPDATED=0
TOTAL_SKIPPED=0
TOTAL_ERRORS=0

# First, get the total count
RESPONSE=$(curl -s -w "\n%{http_code}" \
    "${API_BASE_URL}/raid/" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Accept: application/json")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" != "200" ]; then
    echo "ERROR: Failed to fetch raid list (HTTP $HTTP_CODE)"
    echo "$BODY" | head -5
    exit 1
fi

# Get handles from the list response
echo "$BODY" | jq -r '.[].identifier.id' > "$HANDLES_FILE" 2>/dev/null

TOTAL=$(wc -l < "$HANDLES_FILE" | tr -d ' ')
echo "Found $TOTAL raids to check"
echo ""

while IFS= read -r RAID_ID; do
    TOTAL_PROCESSED=$((TOTAL_PROCESSED + 1))

    # Extract prefix and suffix from the identifier URL
    # e.g. https://raid.org/10.26259/abc123 -> 10.26259/abc123
    # Extract prefix and suffix from the last two path segments of the identifier URL
    # e.g. https://raid.org/10.26259/abc123 -> prefix=10.26259, suffix=abc123
    #       http://raid.local/102.100.100/447201 -> prefix=102.100.100, suffix=447201
    SUFFIX=$(basename "$RAID_ID")
    PREFIX=$(basename "$(dirname "$RAID_ID")")

    # Refresh token every 100 raids (tokens expire)
    if [ $((TOTAL_PROCESSED % 100)) -eq 0 ]; then
        ACCESS_TOKEN=$(get_token)
        if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
            echo "ERROR: Failed to refresh token at raid $TOTAL_PROCESSED"
            exit 1
        fi
    fi

    # GET the raid
    RAID_RESPONSE=$(curl -s -w "\n%{http_code}" \
        "${API_BASE_URL}/raid/${PREFIX}/${SUFFIX}" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Accept: application/json")

    RAID_HTTP_CODE=$(echo "$RAID_RESPONSE" | tail -1)
    RAID_BODY=$(echo "$RAID_RESPONSE" | sed '$d')

    if [ "$RAID_HTTP_CODE" != "200" ]; then
        echo "[$TOTAL_PROCESSED/$TOTAL] ERROR: GET ${PREFIX}/${SUFFIX} returned HTTP $RAID_HTTP_CODE"
        TOTAL_ERRORS=$((TOTAL_ERRORS + 1))
        continue
    fi

    # Check if this raid has any github.com vocabulary URIs
    if ! echo "$RAID_BODY" | grep -q "github.com/au-research/raid-metadata"; then
        TOTAL_SKIPPED=$((TOTAL_SKIPPED + 1))
        continue
    fi

    # Apply URI replacements
    UPDATED_RAID=$(echo "$RAID_BODY" | jq "$JQ_FILTER")

    if [ $? -ne 0 ]; then
        echo "[$TOTAL_PROCESSED/$TOTAL] ERROR: jq transform failed for ${PREFIX}/${SUFFIX}"
        TOTAL_ERRORS=$((TOTAL_ERRORS + 1))
        continue
    fi

    # Verify no github.com URIs remain
    if echo "$UPDATED_RAID" | grep -q "github.com/au-research/raid-metadata"; then
        echo "[$TOTAL_PROCESSED/$TOTAL] WARNING: Residual github.com URIs in ${PREFIX}/${SUFFIX} after transform"
        TOTAL_ERRORS=$((TOTAL_ERRORS + 1))
        continue
    fi

    if [ "$DRY_RUN" = true ]; then
        TOTAL_UPDATED=$((TOTAL_UPDATED + 1))
        echo "[$TOTAL_PROCESSED/$TOTAL] WOULD UPDATE: ${PREFIX}/${SUFFIX}"
        continue
    fi

    # PUT the updated raid
    PUT_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -X PUT "${API_BASE_URL}/raid/${PREFIX}/${SUFFIX}" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$UPDATED_RAID")

    PUT_HTTP_CODE=$(echo "$PUT_RESPONSE" | tail -1)
    PUT_BODY=$(echo "$PUT_RESPONSE" | sed '$d')

    if [ "$PUT_HTTP_CODE" = "200" ]; then
        TOTAL_UPDATED=$((TOTAL_UPDATED + 1))
        echo "[$TOTAL_PROCESSED/$TOTAL] UPDATED: ${PREFIX}/${SUFFIX}"
    else
        echo "[$TOTAL_PROCESSED/$TOTAL] ERROR: PUT ${PREFIX}/${SUFFIX} returned HTTP $PUT_HTTP_CODE"
        echo "  Response: $(echo "$PUT_BODY" | head -c 200)"
        TOTAL_ERRORS=$((TOTAL_ERRORS + 1))
    fi

    sleep 0.2
done < "$HANDLES_FILE"

echo ""
echo "=== Summary ==="
echo "Total checked:  $TOTAL_PROCESSED"
echo "Updated:        $TOTAL_UPDATED"
echo "Skipped (ok):   $TOTAL_SKIPPED"
echo "Errors:         $TOTAL_ERRORS"
