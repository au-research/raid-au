#!/bin/bash
#
# generate-subject-mapping.sh
#
# Description:
#   This script fetches subject mapping data from ARDC vocabularies and processes it
#   into a standardized JSON format for use in both the RAID agency app and static app.
#   It extracts subject type information including keys, values, and definitions.
#
# Usage:
#   ./generate-subject-mapping.sh
#
# Dependencies:
#   - curl: For fetching data from the ARDC API
#   - jq: For processing JSON data
#
# Output:
#   - JSON file containing the processed subject mapping data in both app locations:
#     - raid-agency-app/src/mapping/data/subject-mapping.json
#     - raid-agency-app-static/src/mapping/data/subject-mapping.json
#

# Check if we're running the script from the correct directory
if [[ $(basename $(pwd)) != "scripts" ]]; then
    echo "Error: Please run this script from the 'scripts' directory"
    exit 1
fi

# Define constants for file paths and API URL
TEMP_FILE="/tmp/subject-mapping.json"  # Temporary file for storing the generated JSON
FINAL_PATH_APP="../raid-agency-app/src/mapping/data/subject-mapping.json"  # Path for the agency app
FINAL_PATH_STATIC="../raid-agency-app-static/src/mapping/data/subject-mapping.json"  # Path for the static app
URL="https://vocabs.ardc.edu.au/registry/api/resource/versions/536/versionArtefacts/conceptTree"  # ARDC vocabulary API URL

# Download content from ARDC vocabulary API and process it with jq
# The jq script:
# 1. Extracts concepts from the nested structure (forest → children → children)
# 2. Filters for entries with type "concept"
# 3. Creates a standardized format with field, key, value, and definition
# 4. Sorts the result by key for consistent ordering
curl -sf "${URL}" | jq '
[
  # Navigate through the nested structure and select concepts
  .forest[]?.children[]?.children[]? |
  select(.type == "concept") |
  # Create standardized entry with consistent fields
  {
    field: "subject.type.id",    # Field identifier
    key: .notation,              # Subject code/notation as key
    value: .label,               # Human-readable label as value
    definition: .iri             # URI identifier as definition
  }
] | sort_by(.key)' > "${TEMP_FILE}"

# Check if the processing was successful
# $? -eq 0: Checks if the previous command (curl + jq) exited successfully
# -s "${TEMP_FILE}": Checks if the output file has content (not empty)
if [ $? -eq 0 ] && [ -s "${TEMP_FILE}" ]; then
    # Ensure target directories exist for both applications
    mkdir -p "$(dirname "${FINAL_PATH_APP}")"
    mkdir -p "$(dirname "${FINAL_PATH_STATIC}")"
    
    # Copy the processed file to both final destinations
    cp "${TEMP_FILE}" "${FINAL_PATH_APP}"
    cp "${TEMP_FILE}" "${FINAL_PATH_STATIC}"
    
    # Clean up temporary file after successful processing
    rm -f "${TEMP_FILE}"
    echo "Processing completed successfully"
else
    # If processing failed, report error and clean up any partial temporary file
    echo "Error processing data" >&2
    rm -f "${TEMP_FILE}"
    exit 1
fi