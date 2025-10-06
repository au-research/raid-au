#!/bin/bash

# Check if required parameters are provided
if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <environment> <client_id> <client_secret>"
    echo "Environment must be one of: local, test, demo, stage, prod"
    echo "Example: $0 test raid-upgrader uQdAmNCYOPeEL58sNBiwBqeyKC4evv71"
    exit 1
fi

# Parameters
ENVIRONMENT="$1"
CLIENT_ID="$2"
CLIENT_SECRET="$3"

# Set API_BASE_URL and IAM_BASE_URL based on environment
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

# Auth configuration
TOKEN_URL="${IAM_BASE_URL}/realms/raid/protocol/openid-connect/token"

URL="${API_BASE_URL}/upgrade"

# Get access token
token_response=$(curl -s -X POST "$TOKEN_URL" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=client_credentials" -d "client_id=$CLIENT_ID" -d "client_secret=$CLIENT_SECRET")

echo $token_response

# Extract the access token
access_token=$(echo "$token_response" | jq -r '.access_token')
echo $access_token

if [ -z "$access_token" ]; then
    echo "Failed to obtain access token"
    exit 1
fi

# Make the GET request with the token and store the response
response=$(curl -s "$URL" -H "Authorization: Bearer $access_token")

# Parse the JSON response and iterate through each resource
echo "$response" | jq -c '.[]' | while read -r resource; do
    # Make POST request with the current resource as the body and the token
    curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $access_token" -d "$resource" "$URL"

    # Optional: Add a small delay between requests
    sleep 0.5
done