#!/bin/bash
#
# generate-models.sh
#
# Description:
#   This script generates TypeScript model files from OpenAPI specifications for use 
#   in both the RAID agency app and static app. It uses the OpenAPI generator via Docker
#   to create TypeScript interfaces, then processes and cleans the output files.
#
# Usage:
#   ./generate-models.sh
#
# Dependencies:
#   - Docker: Used to run the OpenAPI generator
#   - uuidgen: For creating unique temporary directories
#   - OpenAPI generator: Run through Docker
#   - Standard bash utilities (find, sed, awk)
#
# Output:
#   - TypeScript model files in:
#     - raid-agency-app/src/generated/raid/
#     - raid-agency-app-static/src/generated/raid/
#

# Check if we're running the script from the correct directory
if [[ $(basename $(pwd)) != "scripts" ]]; then
    echo "Error: Please run this script from the 'scripts' directory"
    exit 1
fi

# Define a unique key for temp directory to avoid conflicts
uuid=$(uuidgen)
key=${uuid%%-*}

# Define paths for temporary and target directories
tempDir="/tmp/raid-${key}"
localPath="$tempDir"
openapiPath="${tempDir}/openapi"  # Where OpenAPI spec files will be copied
outputPath="${tempDir}/output"    # Where generator output will be stored
sourceApiPath="../api-svc/idl-raid-v2/src"  # Source of OpenAPI specs

# Define cleanup function to remove temporary files
cleanup() {
    if [ -d "${tempDir}" ]; then
        rm -rf /tmp/raid*
    fi
}

# Clean up any existing raid temporary directories
rm -rf /tmp/raid*

# Set up trap to ensure cleanup runs even if script fails
trap cleanup EXIT

# Create temporary directories
mkdir -p "${localPath}" "${openapiPath}" "${outputPath}"

# Check if source directory exists
if [ ! -d "${sourceApiPath}" ]; then
    echo "Error: Source directory ${sourceApiPath} does not exist"
    exit 1
fi

# Copy OpenAPI definition files to temporary directory
cp -r "${sourceApiPath}/"* "${openapiPath}/" && echo "Copied src to openapiPath successfully" || { echo "Error copying files"; exit 1; }

# Run OpenAPI generator using Docker to generate TypeScript interfaces
# Parameters:
# --skip-validate-spec: Skip OpenAPI spec validation for faster processing
# -i: Input file path (the OpenAPI specification)
# -g: Generator to use (typescript-fetch)
# --global-property: Only generate models, not docs or APIs
docker run --rm \
    -v "${openapiPath}":/local \
    -v "${outputPath}":/output \
    --user $(id -u):$(id -g) \
    openapitools/openapi-generator-cli:latest generate \
    --skip-validate-spec \
    -i /local/raido-openapi-3.0.yaml \
    -g typescript-fetch \
    --global-property models,modelDocs=false,apis=false,apiDocs=false,supportingFiles=false \
    -o /output

# Process all TypeScript files to clean them up:
# 1. Extract import statements (excluding runtime imports)
# 2. Extract interface definitions (removing comments)
find "${outputPath}" -name "*.ts" -type f | while read -r file; do
    temp_file="${file}.temp"
    (
        # Extract import statements but remove runtime imports
        grep "^import.*;" "$file" | grep -v "'../runtime'"
        echo ""
        # Extract interface definitions and remove comments
        sed -n '/^export interface/,/^}/p' "$file" | sed '/\/\*\*/,/\*\//d'
    ) > "$temp_file"
    mv "$temp_file" "$file"
done

# Define destination paths for both apps
RAID_APP_PATH="../raid-agency-app/src/generated/raid"
STATIC_GEN_PATH="../raid-agency-app-static/src/generated/raid"

# Create/clean destination directories
for dir in "$RAID_APP_PATH" "$STATIC_GEN_PATH"; do
    rm -rf "$dir"
    mkdir -p "$dir" || { echo "Error creating directory $dir"; exit 1; }
done

# Copy generated files to both destination apps
for dest in "$RAID_APP_PATH" "$STATIC_GEN_PATH"; do
    if cp -r "${outputPath}/"* "$dest/"; then
        echo "Copied to ${dest##*/*/} successfully"
    else
        echo "Error copying files to ${dest##*/*/}"
        exit 1
    fi
done

# Move files from models/ subdirectory to the root of each destination
for path in "${RAID_APP_PATH}" "${STATIC_GEN_PATH}"; do
    mv "${path}/models/"* "${path}/" && rm -r "${path}/models"
done

# Create index.ts file for each destination that exports all models
for path in "${RAID_APP_PATH}" "${STATIC_GEN_PATH}"; do
    # Find all .ts files (excluding index.ts itself) and create export statements
    find "${path}" -maxdepth 1 -name "*.ts" ! -name "index.ts" -exec basename {} .ts \; | \
    awk '{print "export * from \"./" $1 "\""}' > "${path}/index.ts"
    echo "Created index.ts in ${path##*/*/}"
done