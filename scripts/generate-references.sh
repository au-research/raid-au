#!/bin/bash
#
# generate-references.sh
#
# Description:
#   This script extracts reference data from the database and saves it as JSON files
#   for use in the RAID agency app. It handles:
#   - Filtering tables based on schema_id and status columns
#   - Filtering language codes based on configuration
#   - Excluding specified tables and columns from processing
#   - Optionally post-processing certain tables (e.g., subject_type.json)
#
# Usage:
#   ./generate-references.sh
#
# Dependencies:
#   - PostgreSQL client (psql)
#   - jq: For processing JSON data
#   - Database connection details (via environment variables)
#
# Environment Variables Required:
#   - PG_USER: Database username
#   - PG_PASS: Database password
#   - PG_HOST: Database host
#   - PG_PORT: Database port (defaults to 5432 if not set)
#   - PG_DB: Database name
#
# Configuration:
#   Uses generate-references-config.json file for:
#   - Language codes to include
#   - Output directory
#   - Tables and columns to ignore
#

# Check if we're running the script from the correct directory
if [[ $(basename $(pwd)) != "scripts" ]]; then
    echo "Error: Please run this script from the 'scripts' directory"
    exit 1
fi

# Load environment variables from .env file if it exists
if [ -f .env ]; then
    export $(cat .env | xargs)
fi

# Define paths for temporary output and configuration
OUTPUT_DIR="/tmp/raid-references"
CONFIG_FILE="$(dirname "$0")/generate-references-config.json"

# Construct database connection string with PostgreSQL credentials
DB_CONN="postgresql://$PG_USER:$PG_PASS@$PG_HOST:${PG_PORT:-5432}/$PG_DB"

# Read configuration using jq:
# - Get language codes as comma-separated quoted strings for SQL
# - Get final destination directory
LANGUAGE_CODES=$(jq -r '.language_codes | map("'\''" + . + "'\''") | join(",")' "$CONFIG_FILE")
FINAL_DIR="$(dirname "$0")/$(jq -r '.final_dir' "$CONFIG_FILE")"

# Extract tables and columns to ignore from configuration:
# - IGNORED_TABLES: Complete tables to skip
# - IGNORED_COLUMNS: Specific columns to skip across all tables
IGNORED_TABLES=$(jq -r '.ignore_tables[] | select(type=="string")' "$CONFIG_FILE" | while read table; do echo "'$table'"; done | paste -sd "," -)
IGNORED_COLUMNS=$(jq -r '.ignore_tables[] | select(type=="object") | .columns[]' "$CONFIG_FILE" | while read col; do echo "'$col'"; done | paste -sd "," -)

# Create/recreate output directory
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

# Function to process tables based on column name and status condition
# Parameters:
#   $1: column_name - The column to filter tables by (e.g., schema_id, status)
#   $2: status_condition - Optional condition for status-based filtering
process_tables() {
    local column_name="$1"
    local status_condition="$2"

    # Get tables with specified column, excluding ignored tables and columns
    # For status columns, add additional filter on data type (schema_status)
    local schema_filter=""
    if [ "$column_name" = "status" ]; then
        schema_filter="AND udt_name='schema_status'"
    fi

    # Build SQL conditions for ignored tables and columns
    # These are dynamically included in the query if set in config
    local ignore_tables_condition=""
    if [ ! -z "$IGNORED_TABLES" ]; then
        ignore_tables_condition="AND c1.table_name NOT IN ($IGNORED_TABLES)"
    fi

    local ignore_columns_condition=""
    if [ ! -z "$IGNORED_COLUMNS" ]; then
        # Create a subquery to exclude tables that have columns we want to ignore
        ignore_columns_condition="AND NOT EXISTS (
            SELECT 1 
            FROM information_schema.columns c2 
            WHERE c2.table_schema = 'api_svc' 
            AND c2.table_name = c1.table_name 
            AND c2.column_name IN ($IGNORED_COLUMNS)
        )"
    fi

    # Find all tables in the api_svc schema that have our target column
    # and don't match any of our exclusion filters
    tables=$(psql "$DB_CONN" -t -A -c "
        SELECT DISTINCT c1.table_name
        FROM information_schema.columns c1
        WHERE c1.table_schema = 'api_svc'
        AND c1.column_name = '$column_name'
        $schema_filter
        $ignore_tables_condition
        $ignore_columns_condition
    ")

    # Process each table to extract reference data
    for table in $tables; do
        # Special handling for language table - filter by configured languages
        # and ensure we get the newest entry for each language
        if [ "$table" = "language" ]; then
            query="SELECT json_agg(t) FROM (
                SELECT DISTINCT ON (code) *
                FROM api_svc.language
                WHERE code IN ($LANGUAGE_CODES)
                ORDER BY code, name, schema_id DESC
            ) t"
        # For schema_id tables, get only the records with the latest schema version
        elif [ "$column_name" = "schema_id" ]; then
            query="SELECT json_agg(t) FROM (
                SELECT * FROM api_svc.$table
                WHERE schema_id = (SELECT DISTINCT MAX(schema_id) FROM api_svc.$table)
            ) t"
        # For status-based tables (e.g., active records only)
        else
            query="SELECT json_agg(t) FROM (
                SELECT * FROM api_svc.$table WHERE $status_condition
            ) t"
        fi

        # Execute query and save results as JSON with pretty formatting
        psql "$DB_CONN" -t -A -c "$query" | jq '.' > "$OUTPUT_DIR/${table}.json"
        echo "Created $OUTPUT_DIR/${table}.json"
    done
}

# Post-processing function for subject_type.json
# This function modifies the subject type IDs to use the full URI format
post_processing() {
    local file="$OUTPUT_DIR/subject_type.json"
    if [ -f "$file" ]; then
        # Using jq to modify the JSON - prepend the base URI to each ID
        jq '
            map(. + {
                id: "https://linked.data.gov.au/def/anzsrc-for/2020/\(.id)"
            })
        ' "$file" > "${file}.tmp" && mv "${file}.tmp" "$file"
    fi
}

# Main execution block with error handling
{
    # Check if configuration file exists
    if [ ! -f "$CONFIG_FILE" ]; then
        echo "Config file not found: $CONFIG_FILE"
        exit 1
    fi

    # Process tables filtered by schema_id column
    # These are typically tables with versioned schema data
    process_tables "schema_id"

    # Process tables filtered by status column with 'active' status
    # These are typically tables with status-tracked records
    process_tables "status" "status = 'active'"

    # Post-processing for specific tables 
    # Currently disabled but can be enabled by uncommenting
    # Adds URI prefixes to subject type IDs
    # post_processing

    # Copy the generated reference files to the final destination
    # First clean the target directory, then create it
    rm -rf "$FINAL_DIR"
    mkdir -p "$FINAL_DIR"
    cp -r "$OUTPUT_DIR"/* "$FINAL_DIR/"
    echo "Copied references to $FINAL_DIR"
} || {
    # Error handling for any failure in the main execution block
    echo "An error occurred during execution"
    exit 1
}