#!/bin/bash

# Check if we're running the script from the correct directory
if [[ $(basename $(pwd)) != "scripts" ]]; then
    echo "Error: Please run this script from the 'scripts' directory"
    exit 1
fi

# Define constants
TEMP_FILE="/tmp/subject-mapping.json"
FINAL_PATH_APP="../raid-agency-app/src/mapping/data/subject-mapping.json"
FINAL_PATH_STATIC="../raid-agency-app-static/src/mapping/data/subject-mapping.json"
URL="https://vocabs.ardc.edu.au/registry/api/resource/versions/536/versionArtefacts/conceptTree"

# Download and process in one step
curl -sf "${URL}" | jq '
[
  .forest[]?.children[]?.children[]? |
  select(.type == "concept") |
  {
    field: "subject.type.id",
    key: .notation,
    value: .label,
    definition: .iri
  }
] | sort_by(.key)' > "${TEMP_FILE}"

# Check if the processing was successful
if [ $? -eq 0 ] && [ -s "${TEMP_FILE}" ]; then
    # Ensure target directories exist
    mkdir -p "$(dirname "${FINAL_PATH_APP}")"
    mkdir -p "$(dirname "${FINAL_PATH_STATIC}")"
    
    # Copy to both final destinations
    cp "${TEMP_FILE}" "${FINAL_PATH_APP}"
    cp "${TEMP_FILE}" "${FINAL_PATH_STATIC}"
    
    # Clean up temp file
    rm -f "${TEMP_FILE}"
    echo "Processing completed successfully"
else
    echo "Error processing data" >&2
    rm -f "${TEMP_FILE}"
    exit 1
fi