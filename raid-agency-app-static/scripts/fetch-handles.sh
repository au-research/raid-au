#!/bin/bash
# Output file
OUTPUT_FILE="src/raw-data/all-handles.json"
# Temporary files for processing
TEMP_FILE=$(mktemp)
RESULT_FILE=$(mktemp)
# Initialize empty array for the new format
echo "[]" >"$RESULT_FILE"
# Fetch and process data from each backend
fetch_backend() {
  local backend=$1
  local url=$2
  echo "Fetching data from $backend: $url"
  # Fetch data
  local response=$(curl -s "$url")
  # Verify we got a response
  if [ -z "$response" ]; then
    echo "Warning: Empty response from $backend"
    return
  fi
  # Process JSON array directly
  echo "$response" | jq -r '.[]' 2>/dev/null | while read -r handle; do
    if [ -n "$handle" ]; then
      # Add to result as [key, value] pair
      jq --arg handle "$handle" --arg backend "$backend" '. += [[$handle, $backend]]' "$RESULT_FILE" >"$TEMP_FILE"
      cat "$TEMP_FILE" >"$RESULT_FILE"
    fi
  done
  # Check if result file was updated
  echo "Current size of result file: $(wc -c <"$RESULT_FILE") bytes"
}
# Process each backend
fetch_backend "prod" "https://static.prod.raid.org.au/api/handles.json"
fetch_backend "stage" "https://static.stage.raid.org.au/api/handles.json"
fetch_backend "demo" "https://static.demo.raid.org.au/api/handles.json"
fetch_backend "test" "https://static.test.raid.org.au/api/handles.json"
# Save final result to output file
cat "$RESULT_FILE" >"$OUTPUT_FILE"
echo "Combined handles saved to $OUTPUT_FILE"
echo "Final file size: $(wc -c <"$OUTPUT_FILE") bytes"
# Clean up
rm -f "$TEMP_FILE" "$RESULT_FILE"
