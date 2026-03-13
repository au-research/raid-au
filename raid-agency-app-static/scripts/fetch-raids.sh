#!/bin/bash

# RAID Data Collection Script
#
# This script fetches raid data from the designated API.
# It performs the following operations:
# 1. Loads environment variables from a .env file
# 2. Validates required environment variables are present
# 3. Authenticates with the IAM endpoint to obtain a bearer token
# 4. Fetches all public raid data from the API
# 5. Saves the response to raids.json
#
# The script creates:
# - Current data: ./src/raw-data/raids.json
#

# Check if jq is installed
if ! command -v jq &>/dev/null; then
  echo "Error: jq is not installed. Please install it with: sudo apt-get install jq"
  exit 1
fi

if [ -f ".env" ]; then
  echo "Loading environment variables from .env file"
  export $(grep -v '^#' .env | xargs)
else
  echo "No .env file found"
fi

# Required environment variables
required_vars=("IAM_ENDPOINT" "API_ENDPOINT" "IAM_CLIENT_ID" "IAM_CLIENT_SECRET" "RAID_ENV")

# Check if all required environment variables are set
missing_vars=()
for var in "${required_vars[@]}"; do
  if [ -z "${!var}" ]; then
    missing_vars+=("$var")
  fi
done

# If any variables are missing, show error and exit
if [ ${#missing_vars[@]} -ne 0 ]; then
  echo "Error: The following required environment variables are not set:"
  for var in "${missing_vars[@]}"; do
    echo "  - $var"
  done
  echo "Please set these variables before running the script."
  exit 1
fi

DATA_DIR="./src/raw-data"

# Create the data directory if it doesn't exist
if [ ! -d "$DATA_DIR" ]; then
  echo "Creating directory $DATA_DIR"
  mkdir -p "$DATA_DIR"
fi

# Main output file (always named raids.json)
OUTPUT_FILE="$DATA_DIR/raids.json"

# Step 1: Get bearer token
echo "Getting bearer token..."
TOKEN_RESPONSE=$(curl -s -X POST $IAM_ENDPOINT/realms/raid/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=$IAM_CLIENT_ID&client_secret=$IAM_CLIENT_SECRET")

# Extract token from response
BEARER_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.access_token')

if [ -z "$BEARER_TOKEN" ] || [ "$BEARER_TOKEN" == "null" ]; then
  echo "Failed to get bearer token. Response: $TOKEN_RESPONSE"
  exit 1
fi

echo "Token acquired successfully."

# Step 2: Fetch the raid data from the API
echo "Fetching data from API..."
API_RESPONSE=$(curl -s -X GET $API_ENDPOINT/raid/all-public \
  -H "Authorization: Bearer $BEARER_TOKEN" \
  -H "Content-Type: application/json")

# Step 3: Save response to JSON file
echo $API_RESPONSE >$OUTPUT_FILE

if [ $? -eq 0 ]; then
  echo "Data successfully saved to $OUTPUT_FILE"
else
  echo "Error saving data to file."
  exit 1
fi

# Make sure the data directory exists for handles.json as well
output_dir=$(dirname "src/raw-data/handles.json")
if [ ! -d "$output_dir" ]; then
  echo "Creating directory $output_dir"
  mkdir -p "$output_dir"
fi

output_file="src/raw-data/handles.json"

# Use jq to extract the handles and create a JSON array directly
jq -r '.[].identifier.id' $OUTPUT_FILE |
  sed -E 's|http://[^/]*/([^/]+/[^/]+).*|\1|' |
  sort |
  uniq |
  jq -R . |
  jq -s . >"$output_file"

echo "Unique handles have been saved to $output_file as a JSON array"
