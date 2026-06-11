#!/bin/bash

# Updates raid vocabulary URIs from old github-based URIs to new vocabulary.raid.org URIs.
# Automatically finds all raids that use github-based vocabulary URIs and updates them.
#
# Handles: title type, description type, access type, contributor position,
#          organisation role, related raid type, related object type, related object category.
#
# All mappings sourced from RaidUpgradeService.java.
#
# Usage: ./update-title-types.sh <environment> <client_id> <client_secret>
# Example: ./update-title-types.sh demo raid-upgrader secret123

if [ "$#" -lt 3 ]; then
    echo "Usage: $0 <environment> <client_id> <client_secret>"
    echo "Environment must be one of: local, test, demo, stage, prod"
    echo "Example: $0 demo raid-upgrader secret123"
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
        echo "Environment must be one of: local, test, demo, stage, prod"
        exit 1
        ;;
esac

echo "Using environment: $ENVIRONMENT"
echo "API Base URL: $API_BASE_URL"
echo "IAM Base URL: $IAM_BASE_URL"
echo ""

# Get access token
TOKEN_URL="${IAM_BASE_URL}/realms/raid/protocol/openid-connect/token"
token_response=$(curl -s -X POST "$TOKEN_URL" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=client_credentials" \
    -d "client_id=$CLIENT_ID" \
    -d "client_secret=$CLIENT_SECRET")

access_token=$(echo "$token_response" | jq -r '.access_token')

if [ -z "$access_token" ] || [ "$access_token" = "null" ]; then
    echo "Failed to obtain access token"
    echo "$token_response"
    exit 1
fi

echo "Access token obtained"
echo ""

# Fetch all raids and find those with github-based vocabulary URIs
echo "Fetching all raids to find those with github-based vocabulary URIs..."

all_raids=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $access_token" \
    -H "Accept: application/json" \
    "${API_BASE_URL}/raid/")

list_http_code=$(echo "$all_raids" | tail -1)
list_body=$(echo "$all_raids" | sed '$d')

if [ "$list_http_code" != "200" ]; then
    echo "FAILED to fetch raid list (HTTP $list_http_code)"
    echo "$list_body"
    exit 1
fi

# Extract handles of raids that contain github-based vocabulary URIs anywhere in their JSON
GITHUB_MARKER="github.com/au-research/raid-metadata"
handles=$(echo "$list_body" | jq -r --arg marker "$GITHUB_MARKER" '
    [.[] | select(tostring | contains($marker)) | .identifier.id] |
    unique | .[]
')

if [ -z "$handles" ]; then
    echo "No raids found with github-based vocabulary URIs."
    exit 0
fi

handle_count=$(echo "$handles" | wc -l | tr -d ' ')
echo "Found $handle_count raid(s) with github-based vocabulary URIs."
echo ""

# Build jq mapping objects as JSON for use with --argjson.
# This approach handles the large number of related object type mappings cleanly.

GITHUB_BASE="https://github.com/au-research/raid-metadata/blob/main/scheme"
GITHUB_SCHEMA_BASE="https://github.com/au-research/raid-metadata/tree/main/scheme"

# Title type
TITLE_TYPE_MAP='{
    "'"$GITHUB_BASE"'/title/type/v1/primary.json": "https://vocabulary.raid.org/title.type.schema/5",
    "'"$GITHUB_BASE"'/title/type/v1/alternative.json": "https://vocabulary.raid.org/title.type.schema/4"
}'
TITLE_SCHEMA_MAP='{
    "'"$GITHUB_SCHEMA_BASE"'/title/type/v1/": "https://vocabulary.raid.org/title.type.schema/376"
}'

# Description type
DESC_TYPE_MAP='{
    "'"$GITHUB_BASE"'/description/type/v1/primary.json": "https://vocabulary.raid.org/description.type.schema/318",
    "'"$GITHUB_BASE"'/description/type/v1/alternative.json": "https://vocabulary.raid.org/description.type.schema/319"
}'
DESC_SCHEMA_MAP='{
    "'"$GITHUB_SCHEMA_BASE"'/description/type/v1/": "https://vocabulary.raid.org/description.type.schema/320"
}'

# Access type
ACCESS_TYPE_MAP='{
    "'"$GITHUB_BASE"'/access/type/v1/closed.json": "https://vocabularies.coar-repositories.org/access_rights/c_f1cf/",
    "'"$GITHUB_BASE"'/access/type/v1/embargoed.json": "https://vocabularies.coar-repositories.org/access_rights/c_f1cf/",
    "'"$GITHUB_BASE"'/access/type/v1/open.json": "https://vocabularies.coar-repositories.org/access_rights/c_abf2/"
}'
ACCESS_SCHEMA_MAP='{
    "'"$GITHUB_SCHEMA_BASE"'/access/type/v1/": "https://vocabularies.coar-repositories.org/access_rights/"
}'

# Contributor position
# Note: leader.json and contact-person.json are handled specially by RaidUpgradeService
# (converted to boolean flags), so they are not mapped here.
CONTRIB_POS_MAP='{
    "'"$GITHUB_BASE"'/contributor/position/v1/co-investigator.json": "https://vocabulary.raid.org/contributor.position.schema/308",
    "'"$GITHUB_BASE"'/contributor/position/v1/other-participant.json": "https://vocabulary.raid.org/contributor.position.schema/311",
    "'"$GITHUB_BASE"'/contributor/position/v1/principal-investigator.json": "https://vocabulary.raid.org/contributor.position.schema/307"
}'
CONTRIB_POS_SCHEMA_MAP='{
    "'"$GITHUB_SCHEMA_BASE"'/contributor/position/v1/": "https://vocabulary.raid.org/contributor.position.schema/305"
}'

# Organisation role
ORG_ROLE_MAP='{
    "'"$GITHUB_BASE"'/organisation/role/v1/contractor.json": "https://vocabulary.raid.org/organisation.role.schema/185",
    "'"$GITHUB_BASE"'/organisation/role/v1/lead-research-organisation.json": "https://vocabulary.raid.org/organisation.role.schema/182",
    "'"$GITHUB_BASE"'/organisation/role/v1/other-organisation.json": "https://vocabulary.raid.org/organisation.role.schema/188",
    "'"$GITHUB_BASE"'/organisation/role/v1/other-research-organisation.json": "https://vocabulary.raid.org/organisation.role.schema/183",
    "'"$GITHUB_BASE"'/organisation/role/v1/partner-organisation.json": "https://vocabulary.raid.org/organisation.role.schema/184"
}'
ORG_ROLE_SCHEMA_MAP='{
    "'"$GITHUB_SCHEMA_BASE"'/organisation/role/v1/": "https://vocabulary.raid.org/organisation.role.schema/359"
}'

# Related raid type
RELATED_RAID_TYPE_MAP='{
    "'"$GITHUB_BASE"'/related-raid/type/v1/continues.json": "https://vocabulary.raid.org/relatedRaid.type.schema/204",
    "'"$GITHUB_BASE"'/related-raid/type/v1/has-part.json": "https://vocabulary.raid.org/relatedRaid.type.schema/201",
    "'"$GITHUB_BASE"'/related-raid/type/v1/is-continued-by.json": "https://vocabulary.raid.org/relatedRaid.type.schema/203",
    "'"$GITHUB_BASE"'/related-raid/type/v1/is-derived-from.json": "https://vocabulary.raid.org/relatedRaid.type.schema/200",
    "'"$GITHUB_BASE"'/related-raid/type/v1/is-identical-to.json": "https://vocabulary.raid.org/relatedRaid.type.schema/204",
    "'"$GITHUB_BASE"'/related-raid/type/v1/is-obsoleted-by.json": "https://vocabulary.raid.org/relatedRaid.type.schema/205",
    "'"$GITHUB_BASE"'/related-raid/type/v1/is-part-of.json": "https://vocabulary.raid.org/relatedRaid.type.schema/202",
    "'"$GITHUB_BASE"'/related-raid/type/v1/is-source-of.json": "https://vocabulary.raid.org/relatedRaid.type.schema/199",
    "'"$GITHUB_BASE"'/related-raid/type/v1/obsoletes.json": "https://vocabulary.raid.org/relatedRaid.type.schema/198"
}'
RELATED_RAID_TYPE_SCHEMA_MAP='{
    "'"$GITHUB_SCHEMA_BASE"'/related-raid/type/v1/": "https://vocabulary.raid.org/relatedRaid.type.schema/367"
}'

# Related object type (26 mappings)
RELATED_OBJ_TYPE_MAP='{
    "'"$GITHUB_BASE"'/related-object/type/v1/audiovisual.json": "https://vocabulary.raid.org/relatedObject.type.schema/273",
    "'"$GITHUB_BASE"'/related-object/type/v1/book-chapter.json": "https://vocabulary.raid.org/relatedObject.type.schema/271",
    "'"$GITHUB_BASE"'/related-object/type/v1/book.json": "https://vocabulary.raid.org/relatedObject.type.schema/258",
    "'"$GITHUB_BASE"'/related-object/type/v1/computational-notebook.json": "https://vocabulary.raid.org/relatedObject.type.schema/256",
    "'"$GITHUB_BASE"'/related-object/type/v1/conference-paper.json": "https://vocabulary.raid.org/relatedObject.type.schema/264",
    "'"$GITHUB_BASE"'/related-object/type/v1/conference-poster.json": "https://vocabulary.raid.org/relatedObject.type.schema/248",
    "'"$GITHUB_BASE"'/related-object/type/v1/conference-proceeding.json": "https://vocabulary.raid.org/relatedObject.type.schema/262",
    "'"$GITHUB_BASE"'/related-object/type/v1/data-paper.json": "https://vocabulary.raid.org/relatedObject.type.schema/255",
    "'"$GITHUB_BASE"'/related-object/type/v1/dataset.json": "https://vocabulary.raid.org/relatedObject.type.schema/269",
    "'"$GITHUB_BASE"'/related-object/type/v1/dissertation.json": "https://vocabulary.raid.org/relatedObject.type.schema/253",
    "'"$GITHUB_BASE"'/related-object/type/v1/educational-material.json": "https://vocabulary.raid.org/relatedObject.type.schema/267",
    "'"$GITHUB_BASE"'/related-object/type/v1/event.json": "https://vocabulary.raid.org/relatedObject.type.schema/260",
    "'"$GITHUB_BASE"'/related-object/type/v1/funding.json": "https://vocabulary.raid.org/relatedObject.type.schema/272",
    "'"$GITHUB_BASE"'/related-object/type/v1/image.json": "https://vocabulary.raid.org/relatedObject.type.schema/257",
    "'"$GITHUB_BASE"'/related-object/type/v1/instrument.json": "https://vocabulary.raid.org/relatedObject.type.schema/266",
    "'"$GITHUB_BASE"'/related-object/type/v1/journal-article.json": "https://vocabulary.raid.org/relatedObject.type.schema/250",
    "'"$GITHUB_BASE"'/related-object/type/v1/model.json": "https://vocabulary.raid.org/relatedObject.type.schema/263",
    "'"$GITHUB_BASE"'/related-object/type/v1/output-management-plan.json": "https://vocabulary.raid.org/relatedObject.type.schema/247",
    "'"$GITHUB_BASE"'/related-object/type/v1/physical-object.json": "https://vocabulary.raid.org/relatedObject.type.schema/270",
    "'"$GITHUB_BASE"'/related-object/type/v1/preprint.json": "https://vocabulary.raid.org/relatedObject.type.schema/254",
    "'"$GITHUB_BASE"'/related-object/type/v1/prize.json": "https://vocabulary.raid.org/relatedObject.type.schema/268",
    "'"$GITHUB_BASE"'/related-object/type/v1/report.json": "https://vocabulary.raid.org/relatedObject.type.schema/252",
    "'"$GITHUB_BASE"'/related-object/type/v1/service.json": "https://vocabulary.raid.org/relatedObject.type.schema/274",
    "'"$GITHUB_BASE"'/related-object/type/v1/software.json": "https://vocabulary.raid.org/relatedObject.type.schema/259",
    "'"$GITHUB_BASE"'/related-object/type/v1/sound.json": "https://vocabulary.raid.org/relatedObject.type.schema/261",
    "'"$GITHUB_BASE"'/related-object/type/v1/standard.json": "https://vocabulary.raid.org/relatedObject.type.schema/251",
    "'"$GITHUB_BASE"'/related-object/type/v1/text.json": "https://vocabulary.raid.org/relatedObject.type.schema/265",
    "'"$GITHUB_BASE"'/related-object/type/v1/workflow.json": "https://vocabulary.raid.org/relatedObject.type.schema/249"
}'
RELATED_OBJ_TYPE_SCHEMA_MAP='{
    "'"$GITHUB_SCHEMA_BASE"'/related-object/type/v1/": "https://vocabulary.raid.org/relatedObject.type.schema/329"
}'

# Related object category
RELATED_OBJ_CAT_MAP='{
    "'"$GITHUB_BASE"'/related-object/category/v1/input.json": "https://vocabulary.raid.org/relatedObject.category.id/191",
    "'"$GITHUB_BASE"'/related-object/category/v1/internal.json": "https://vocabulary.raid.org/relatedObject.category.id/192",
    "'"$GITHUB_BASE"'/related-object/category/v1/output.json": "https://vocabulary.raid.org/relatedObject.category.id/190"
}'
RELATED_OBJ_CAT_SCHEMA_MAP='{
    "'"$GITHUB_SCHEMA_BASE"'/related-object/category/v1/": "https://vocabulary.raid.org/relatedObject.category.schemaUri/386"
}'

# Language schema
LANGUAGE_SCHEMA_MAP='{
    "https://iso639-3.sil.org": "https://www.iso.org/standard/74575.html"
}'

# Subject schema
SUBJECT_SCHEMA_MAP='{
    "https://linked.data.gov.au/def/anzsrc-for/2020/": "https://vocabs.ardc.edu.au/viewById/316"
}'

# jq filter to transform a single raid's vocabulary URIs
JQ_TRANSFORM='
    # Helper: look up a value in a map, return original if not found
    def map_value($map): . as $v | if $map[$v] then $map[$v] else $v end;

    # Title types
    .title = [.title[] |
        .type.id = (.type.id | map_value($title_type_map)) |
        .type.schemaUri = (.type.schemaUri | map_value($title_schema_map))
    ] |

    # Description types
    if .description then
        .description = [.description[] |
            .type.id = (.type.id | map_value($desc_type_map)) |
            .type.schemaUri = (.type.schemaUri | map_value($desc_schema_map))
        ]
    else . end |

    # Access type
    if .access.type.id then
        .access.type.id = (.access.type.id | map_value($access_type_map)) |
        .access.type.schemaUri = (.access.type.schemaUri | map_value($access_schema_map))
    else . end |

    # Contributor positions
    # leader.json and contact-person.json are converted to boolean flags (not mapped to new URIs)
    if .contributor then
        .contributor = [.contributor[] |
            if .position then
                # Set leader flag if any position has leader.json
                (if ([.position[] | select(.id | contains("leader.json"))] | length > 0) then .leader = true else . end) |
                # Set contact flag if any position has contact-person.json
                (if ([.position[] | select(.id | contains("contact-person.json"))] | length > 0) then .contact = true else . end) |
                # Capture start date from first position before filtering
                (.position[0].startDate // null) as $fallback_start |
                # Remove leader.json and contact-person.json positions, then map remaining
                .position = [.position[] |
                    select(.id | (contains("leader.json") or contains("contact-person.json")) | not) |
                    .id = (.id | map_value($contrib_pos_map)) |
                    .schemaUri = (.schemaUri | map_value($contrib_pos_schema_map))
                ] |
                # If all positions were removed, assign "Other Participant" as default
                if (.position | length) == 0 then
                    .position = [{
                        "schemaUri": "https://vocabulary.raid.org/contributor.position.schema/305",
                        "id": "https://vocabulary.raid.org/contributor.position.schema/311",
                        "startDate": ($fallback_start // "2024-01-01")
                    }]
                else . end
            else . end
        ]
    else . end |

    # Organisation roles
    if .organisation then
        .organisation = [.organisation[] |
            if .role then
                .role = [.role[] |
                    .id = (.id | map_value($org_role_map)) |
                    .schemaUri = (.schemaUri | map_value($org_role_schema_map))
                ]
            else . end
        ]
    else . end |

    # Related raid types
    if .relatedRaid then
        .relatedRaid = [.relatedRaid[] |
            if .type.id then
                .type.id = (.type.id | map_value($rel_raid_type_map)) |
                .type.schemaUri = (.type.schemaUri | map_value($rel_raid_type_schema_map))
            else . end
        ]
    else . end |

    # Related object types and categories
    if .relatedObject then
        .relatedObject = [.relatedObject[] |
            if .type.id then
                .type.id = (.type.id | map_value($rel_obj_type_map)) |
                .type.schemaUri = (.type.schemaUri | map_value($rel_obj_type_schema_map))
            else . end |
            if .category then
                .category = [.category[] |
                    .id = (.id | map_value($rel_obj_cat_map)) |
                    .schemaUri = (.schemaUri | map_value($rel_obj_cat_schema_map))
                ]
            else . end
        ]
    else . end |

    # Language schemas (appears in title, description, access.statement, subject keywords)
    # Helper to map language schemaUri within a language object
    def map_lang: if .language.schemaUri then .language.schemaUri = (.language.schemaUri | map_value($lang_schema_map)) else . end;

    .title = [.title[] | map_lang] |

    if .description then
        .description = [.description[] | map_lang]
    else . end |

    if .access.statement then
        .access.statement = (.access.statement | map_lang)
    else . end |

    # Subject schemas and keywords language
    if .subject then
        .subject = [.subject[] |
            .schemaUri = (.schemaUri | map_value($subject_schema_map)) |
            if .keyword then
                .keyword = [.keyword[] | map_lang]
            else . end
        ]
    else . end
'

updated=0
failed=0
skipped=0

while IFS= read -r raid_id; do
    # Extract prefix/suffix from the identifier URL
    # e.g. https://raid.org/10.82841/abc123 -> prefix=10.82841, suffix=abc123
    # Strip everything up to and including the third slash (scheme + host)
    handle="${raid_id#*://*/}"
    prefix="${handle%/*}"
    suffix="${handle##*/}"

    echo "Processing $prefix/$suffix (${raid_id})"
    url="${API_BASE_URL}/raid/${prefix}/${suffix}"
    # GET the raid
    raid=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer $access_token" \
        -H "Accept: application/json" \
        "$url")

    http_code=$(echo "$raid" | tail -1)
    body=$(echo "$raid" | sed '$d')

    if [ "$http_code" != "200" ]; then
        echo "  FAILED to fetch (HTTP $http_code), skipping"
        failed=$((failed + 1))
        continue
    fi

    # Replace all old github-based URIs with new vocabulary URIs.
    updated_body=$(echo "$body" | jq \
        --argjson title_type_map "$TITLE_TYPE_MAP" \
        --argjson title_schema_map "$TITLE_SCHEMA_MAP" \
        --argjson desc_type_map "$DESC_TYPE_MAP" \
        --argjson desc_schema_map "$DESC_SCHEMA_MAP" \
        --argjson access_type_map "$ACCESS_TYPE_MAP" \
        --argjson access_schema_map "$ACCESS_SCHEMA_MAP" \
        --argjson contrib_pos_map "$CONTRIB_POS_MAP" \
        --argjson contrib_pos_schema_map "$CONTRIB_POS_SCHEMA_MAP" \
        --argjson org_role_map "$ORG_ROLE_MAP" \
        --argjson org_role_schema_map "$ORG_ROLE_SCHEMA_MAP" \
        --argjson rel_raid_type_map "$RELATED_RAID_TYPE_MAP" \
        --argjson rel_raid_type_schema_map "$RELATED_RAID_TYPE_SCHEMA_MAP" \
        --argjson rel_obj_type_map "$RELATED_OBJ_TYPE_MAP" \
        --argjson rel_obj_type_schema_map "$RELATED_OBJ_TYPE_SCHEMA_MAP" \
        --argjson rel_obj_cat_map "$RELATED_OBJ_CAT_MAP" \
        --argjson rel_obj_cat_schema_map "$RELATED_OBJ_CAT_SCHEMA_MAP" \
        --argjson lang_schema_map "$LANGUAGE_SCHEMA_MAP" \
        --argjson subject_schema_map "$SUBJECT_SCHEMA_MAP" \
        "$JQ_TRANSFORM")

    # Check if anything changed
    if [ "$body" = "$updated_body" ]; then
        echo "  No changes needed, skipping"
        skipped=$((skipped + 1))
        continue
    fi

    # Verify no github URIs remain in the updated body
    remaining_github=$(echo "$updated_body" | grep -o "github.com/au-research/raid-metadata[^\"]*" | sort -u)
    if [ -n "$remaining_github" ]; then
        echo "  WARNING: Unmapped github URIs remain:"
        echo "$remaining_github" | while read -r uri; do echo "    $uri"; done
        echo "  Skipping to avoid validation errors"
        failed=$((failed + 1))
        continue
    fi

    # PUT the updated raid
    put_response=$(curl -s -w "\n%{http_code}" -X PUT \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $access_token" \
        -d "$updated_body" \
        "${API_BASE_URL}/raid/${prefix}/${suffix}")

    put_code=$(echo "$put_response" | tail -1)

    if [ "$put_code" = "200" ]; then
        echo "  Updated successfully"
        updated=$((updated + 1))
    else
        put_body=$(echo "$put_response" | sed '$d')
        echo "  FAILED to update (HTTP $put_code): $put_body"
        failed=$((failed + 1))
    fi

    sleep 0.5
done <<< "$handles"

echo ""
echo "Done. Updated: $updated, Skipped: $skipped, Failed: $failed"
