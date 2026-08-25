#!/bin/bash

# RAID-797: Backfill already-minted RAiDs so their DataCite records emit related
# RAiDs with the native "RAiD" relatedIdentifierType instead of generic "DOI".
#
# Only RAiDs that actually have a relatedRaid entry are re-posted; RAiDs with no
# relatedRaid are skipped (nothing about their DataCite record changes).
#
# Idempotent: post-to-datacite triggers a full-document PUT to DataCite, which
# replaces the metadata rather than appending, so re-running produces the
# identical record with no duplicate relatedIdentifier entries.

# Check if exactly 3 arguments are provided
if [ $# -ne 3 ]; then
    echo "Usage: $0 <environment> <clientId> <clientSecret>"
    echo "Environment can be one of: local, test, demo, stage, prod"
    echo "Example: $0 demo raid-upgrader BEbm73S8lkIWBTcm8TlU4gREmUkLXLr0"
    exit 1
fi

# Assign arguments to variables
ENVIRONMENT="$1"
CLIENT_ID="$2"
CLIENT_SECRET="$3"

# Validate environment parameter
case $ENVIRONMENT in
    local)
        TOKEN_URL="http://localhost:8001/realms/raid/protocol/openid-connect/token"
        GET_URL="http://localhost:8080/raid/non-legacy"
        POST_URL="http://localhost:8080/raid/post-to-datacite"
        ;;
    test)
        TOKEN_URL="https://iam.test.raid.org.au/realms/raid/protocol/openid-connect/token"
        GET_URL="https://api.test.raid.org.au/raid/non-legacy"
        POST_URL="https://api.test.raid.org.au/raid/post-to-datacite"
        ;;
    demo)
        TOKEN_URL="https://iam.demo.raid.org.au/realms/raid/protocol/openid-connect/token"
        GET_URL="https://api.demo.raid.org.au/raid/non-legacy"
        POST_URL="https://api.demo.raid.org.au/raid/post-to-datacite"
        ;;
    stage)
        TOKEN_URL="https://iam.stage.raid.org.au/realms/raid/protocol/openid-connect/token"
        GET_URL="https://api.stage.raid.org.au/raid/non-legacy"
        POST_URL="https://api.stage.raid.org.au/raid/post-to-datacite"
        ;;
    prod)
        TOKEN_URL="https://iam.prod.raid.org.au/realms/raid/protocol/openid-connect/token"
        GET_URL="https://api.prod.raid.org.au/raid/non-legacy"
        POST_URL="https://api.prod.raid.org.au/raid/post-to-datacite"
        ;;
    *)
        echo "Error: Invalid environment '$ENVIRONMENT'"
        echo "Environment must be one of: local, test, demo, stage, prod"
        exit 1
        ;;
esac

echo "Using environment: $ENVIRONMENT"
echo "TOKEN_URL: $TOKEN_URL"
echo "GET_URL: $GET_URL"
echo "POST_URL: $POST_URL"

# Helper: fetch a fresh client-credentials access token
get_token() {
    local token_response
    token_response=$(curl -s -X POST "$TOKEN_URL" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=client_credentials" \
        -d "client_id=$CLIENT_ID" \
        -d "client_secret=$CLIENT_SECRET")
    echo "$token_response" | jq -r '.access_token'
}

# Get an initial access token for the listing call
access_token=$(get_token)

if [ -z "$access_token" ] || [ "$access_token" == "null" ]; then
    echo "Failed to obtain access token"
    exit 1
fi

echo

# Fetch all non-legacy RAiDs
response=$(curl -s "$GET_URL" -H "Authorization: Bearer $access_token")

total_count=$(echo "$response" | jq 'length')

# Keep only RAiDs that have at least one relatedRaid entry. RAiDs with no
# relatedRaid are skipped entirely (their DataCite record is left untouched).
related_raids=$(echo "$response" | jq -c '.[] | select(.relatedRaid != null and (.relatedRaid | length) > 0)')

posted_count=$(echo "$related_raids" | grep -c . )
skipped_count=$((total_count - posted_count))

echo "Fetched $total_count non-legacy RAiDs: $posted_count with a relatedRaid to re-post, $skipped_count skipped (no relatedRaid)."
echo

# Re-post each RAiD that has a relatedRaid entry
echo "$related_raids" | while read -r resource; do
    [ -z "$resource" ] && continue

    handle=$(echo "$resource" | jq -r '.identifier.id // "unknown"')
    echo "Posting to DataCite: $handle"

    # Refresh the token per iteration (long runs can outlive a single token)
    access_token=$(get_token)
    if [ -z "$access_token" ] || [ "$access_token" == "null" ]; then
        echo "Failed to obtain access token"
        exit 1
    fi

    post_response=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $access_token" \
        -d "$resource" "$POST_URL")
    echo "  HTTP $post_response"

    # Rate-limit against DataCite's API
    sleep 0.5
done

echo
echo "Backfill complete: attempted $posted_count RAiD(s), skipped $skipped_count with no relatedRaid."
