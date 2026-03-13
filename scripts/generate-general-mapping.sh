#!/bin/bash
#
# generate-general-mapping.sh
#
# Description:
#   This script fetches general mapping data from ARDC vocabularies and processes it
#   into a standardized JSON format for use in both the RAID agency app and static app.
#   The script extracts data for various entity types including access types, contributor 
#   positions, description types, organisation roles, related object categories, etc.
#
# Usage:
#   ./generate-general-mapping.sh
#
# Dependencies:
#   - curl: For fetching data from the ARDC API
#   - jq: For processing JSON data
#
# Output:
#   - JSON file containing the processed mapping data in both app locations:
#     - raid-agency-app/src/mapping/data/general-mapping.json
#     - raid-agency-app-static/src/mapping/data/general-mapping.json
#

# Check if we're running the script from the correct directory
if [[ $(basename $(pwd)) != "scripts" ]]; then
    echo "Error: Please run this script from the 'scripts' directory"
    exit 1
fi

# Define constants
TEMP_FILE="/tmp/general-mapping.json"
FINAL_PATH_APP="../raid-agency-app/src/mapping/data/general-mapping.json"
FINAL_PATH_STATIC="../raid-agency-app-static/src/mapping/data/general-mapping.json"
URL="https://vocabs.ardc.edu.au/registry/api/resource/versions/1484/versionArtefacts/conceptTree"

# Download content from ARDC vocabulary API and process it with jq
# The jq script:
# 1. Defines a helper function 'make_entry' to standardize output format
# 2. Extracts data for various entity types from the concept tree
# 3. Sorts the output by field and value for consistency
curl -sf "${URL}" | jq '
# Define a helper function to create standardized entries
def make_entry(field_name; key_source; value_source):
 {
 field: field_name,
 key: key_source,
 value: value_source,
 definition: (.definition // "")
 };
[
 # Extract access types
 (.forest[] | select(.label == "access.type.id") | .children[] |
 make_entry("access.type.id"; .label; .altLabels[0])),
 
 # Extract contributor positions
 (.forest[] | select(.label == "contributor.position.schema") | .children[] |
 make_entry("contributor.position.id"; .iri; .label)),
 
 # Extract description types
 (.forest[] | select(.label == "description.type.schema") | .children[] |
 make_entry("description.type.id"; .iri; .label)),
 
 # Extract organisation roles
 (.forest[] | select(.label == "organisation.role.schema") | .children[] |
 make_entry("organisation.role.id"; .iri; .label)),
 
 # Extract related object categories
 (.forest[] | select(.label == "relatedObject.category.schema") | .children[] |
 make_entry("relatedObject.category.id"; .iri; .label)),
 
 # Extract related object types
 (.forest[] | select(.label == "relatedObject.type.schema") | .children[] |
 make_entry("relatedObject.type.id"; .iri; .label)),
 
 # Extract related RAID types
 (.forest[] | select(.label == "relatedRaid.type.schema") | .children[] |
 make_entry("relatedRaid.type.schema"; .iri; .label)),
 
 # Extract title types
 (.forest[] | select(.label == "title.type.schema") | .children[] |
 make_entry("title.type.schema"; .iri; .label))
] | sort_by(.field, .value)' > "${TEMP_FILE}"

# Check if the processing was successful
# $? -eq 0: Checks if the previous command (curl + jq) exited successfully
# -s "${TEMP_FILE}": Checks if the output file has content (not empty)
if [ $? -eq 0 ] && [ -s "${TEMP_FILE}" ]; then
    # Ensure target directories exist
    mkdir -p "$(dirname "${FINAL_PATH_APP}")"
    mkdir -p "$(dirname "${FINAL_PATH_STATIC}")"
    
    # Copy the processed file to both final destinations
    cp "${TEMP_FILE}" "${FINAL_PATH_APP}"
    cp "${TEMP_FILE}" "${FINAL_PATH_STATIC}"
    
    # Clean up temporary file
    rm -f "${TEMP_FILE}"
    echo "Processing completed successfully"
else
    # If processing failed, report error and clean up
    echo "Error processing data" >&2
    rm -f "${TEMP_FILE}"
    exit 1
fi