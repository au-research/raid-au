#!/bin/bash
# Check if we're running the script from the correct directory
if [[ $(basename $(pwd)) != "scripts" ]]; then
    echo "Error: Please run this script from the 'scripts' directory"
    exit 1
fi

# Load environment variables from .env file
if [ -f .env ]; then
    export $(cat .env | xargs)
fi

# Define paths
OUTPUT_DIR="/tmp/raid-references"
CONFIG_FILE="$(dirname "$0")/generate-references-config.json"

# Database connection string
DB_CONN="postgresql://$PG_USER:$PG_PASS@$PG_HOST:${PG_PORT:-5432}/$PG_DB"

# Read configuration
LANGUAGE_CODES=$(jq -r '.language_codes | map("'\''" + . + "'\''") | join(",")' "$CONFIG_FILE")
FINAL_DIR="$(dirname "$0")/$(jq -r '.final_dir' "$CONFIG_FILE")"

# Get ignored tables and columns
IGNORED_TABLES=$(jq -r '.ignore_tables[] | select(type=="string")' "$CONFIG_FILE" | while read table; do echo "'$table'"; done | paste -sd "," -)
IGNORED_COLUMNS=$(jq -r '.ignore_tables[] | select(type=="object") | .columns[]' "$CONFIG_FILE" | while read col; do echo "'$col'"; done | paste -sd "," -)

# Create/recreate output directory
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

# Function to process tables based on column name and status condition
process_tables() {
    local column_name="$1"
    local status_condition="$2"

    # Get tables with specified column, excluding ignored tables and columns
    local schema_filter=""
    if [ "$column_name" = "status" ]; then
        schema_filter="AND udt_name='schema_status'"
    fi

    # Build the SQL query with dynamic ignore conditions
    local ignore_tables_condition=""
    if [ ! -z "$IGNORED_TABLES" ]; then
        ignore_tables_condition="AND c1.table_name NOT IN ($IGNORED_TABLES)"
    fi

    local ignore_columns_condition=""
    if [ ! -z "$IGNORED_COLUMNS" ]; then
        ignore_columns_condition="AND NOT EXISTS (
            SELECT 1 
            FROM information_schema.columns c2 
            WHERE c2.table_schema = 'api_svc' 
            AND c2.table_name = c1.table_name 
            AND c2.column_name IN ($IGNORED_COLUMNS)
        )"
    fi

    tables=$(psql "$DB_CONN" -t -A -c "
        SELECT DISTINCT c1.table_name
        FROM information_schema.columns c1
        WHERE c1.table_schema = 'api_svc'
        AND c1.column_name = '$column_name'
        $schema_filter
        $ignore_tables_condition
        $ignore_columns_condition
    ")

    # Process each table
    for table in $tables; do
        if [ "$table" = "language" ]; then
            query="SELECT json_agg(t) FROM (
                SELECT DISTINCT ON (code) *
                FROM api_svc.language
                WHERE code IN ($LANGUAGE_CODES)
                ORDER BY code, name, schema_id DESC
            ) t"
        elif [ "$column_name" = "schema_id" ]; then
            query="SELECT json_agg(t) FROM (
                SELECT * FROM api_svc.$table
                WHERE schema_id = (SELECT DISTINCT MAX(schema_id) FROM api_svc.$table)
            ) t"
        else
            query="SELECT json_agg(t) FROM (
                SELECT * FROM api_svc.$table WHERE $status_condition
            ) t"
        fi

        # Execute query and save results as JSON with pretty printing
        psql "$DB_CONN" -t -A -c "$query" | jq '.' > "$OUTPUT_DIR/${table}.json"
        echo "Created $OUTPUT_DIR/${table}.json"
    done
}

# Post-processing function for subject_type.json
post_processing() {
    local file="$OUTPUT_DIR/subject_type.json"
    if [ -f "$file" ]; then
        # Using jq to modify the JSON
        jq '
            map(. + {
                id: "https://linked.data.gov.au/def/anzsrc-for/2020/\(.id)"
            })
        ' "$file" > "${file}.tmp" && mv "${file}.tmp" "$file"
    fi
}

# Main execution
{
    # Check if config file exists
    if [ ! -f "$CONFIG_FILE" ]; then
        echo "Config file not found: $CONFIG_FILE"
        exit 1
    fi

    # Process tables with schema_id
    process_tables "schema_id"

    # Process tables with status
    process_tables "status" "status = 'active'"

    # Uncomment to enable post-processing
    # post_processing

    # Copy the raid-references directory to final location
    rm -rf "$FINAL_DIR"
    mkdir -p "$FINAL_DIR"
    cp -r "$OUTPUT_DIR"/* "$FINAL_DIR/"
    echo "Copied references to $FINAL_DIR"
} || {
    echo "An error occurred during execution"
    exit 1
}