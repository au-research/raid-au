#!/bin/bash

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
        GET_URL="http://localhost:8080/raid/all-public"
        POST_URL="http://localhost:8080/raid/post-to-datacite"
        ;;
    test)
        TOKEN_URL="https://iam.test.raid.org.au/realms/raid/protocol/openid-connect/token"
        GET_URL="https://api.test.raid.org.au/raid/all-public"
        POST_URL="https://api.test.raid.org.au/raid/post-to-datacite"
        ;;
    demo)
        TOKEN_URL="https://iam.demo.raid.org.au/realms/raid/protocol/openid-connect/token"
        GET_URL="https://api.demo.raid.org.au/raid/all-public"
        POST_URL="https://api.demo.raid.org.au/raid/post-to-datacite"
        ;;
    stage)
        TOKEN_URL="https://iam.stage.raid.org.au/realms/raid/protocol/openid-connect/token"
        GET_URL="https://api.test.stage.org.au/raid/all-public"
        POST_URL="https://api.test.stage.org.au/raid/post-to-datacite"
        ;;
    prod)
        TOKEN_URL="https://iam.prod.raid.org.au/realms/raid/protocol/openid-connect/token"
        GET_URL="https://api.prod.raid.org.au/raid/all-public"
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

# Get access token
token_response=$(curl -s -X POST "$TOKEN_URL" -H "Content-Type: application/x-www-form-urlencoded" -d "grant_type=client_credentials" -d "client_id=$CLIENT_ID" -d "client_secret=$CLIENT_SECRET")

#echo $token_response

# Extract the access token
access_token=$(echo "$token_response" | jq -r '.access_token')
#echo $access_token

if [ -z "$access_token" ]; then
    echo "Failed to obtain access token"
    exit 1
fi

# Make the GET request with the token and store the response
response=$(curl -s "$GET_URL" -H "Authorization: Bearer $access_token")

# Parse the JSON response and iterate through each resource
echo "$response" | jq -c '.[]' | while read -r resource; do
    echo 'Posting to Datacite'
    echo "$resource"
    # Make POST request with the current resource as the body and the token
    post_response=$(curl -vvv -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $access_token" -d "$resource" "$POST_URL")
    echo "Response: $post_response"

    # Optional: Add a small delay between requests
    sleep 0.5
done