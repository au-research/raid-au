# RAID-AU Scripts Documentation

This document provides detailed information about the utility scripts used in the RAID-AU project for data generation, model creation, and reference data management.

## Table of Contents

1. [generate-general-mapping.sh](#generate-general-mappingsh)
2. [generate-models.sh](#generate-modelssh)
3. [generate-references-config.json](#generate-references-configjson)
4. [generate-references.sh](#generate-referencessh)
5. [generate-subject-mapping.sh](#generate-subject-mappingsh)

## generate-general-mapping.sh

### Purpose
Fetches and processes general mapping data from ARDC vocabularies for use in both the RAID agency app and static app.

### Usage
```bash
cd scripts
./generate-general-mapping.sh
```

### Process
1. Verifies the script is run from the correct directory
2. Downloads concept tree data from the ARDC vocabulary API
3. Uses `jq` to process the JSON data, extracting:
   - Access types
   - Contributor positions
   - Description types
   - Organisation roles
   - Related object categories and types
   - Related RAID types
   - Title types
4. Saves the processed data to both the agency app and static app mapping directories

### Output Files
- `raid-agency-app/src/mapping/data/general-mapping.json`
- `raid-agency-app-static/src/mapping/data/general-mapping.json`

### Dependencies
- cURL
- jq (JSON processor)

## generate-models.sh

### Purpose
Generates TypeScript model files from OpenAPI specifications for use in both the RAID agency app and static app.

### Usage
```bash
cd scripts
./generate-models.sh
```

### Process
1. Verifies the script is run from the correct directory
2. Creates a temporary directory with a unique key
3. Copies OpenAPI definitions from the source API path
4. Uses Docker to run the OpenAPI generator to create TypeScript interfaces
5. Processes the generated TypeScript files to extract only imports and interfaces
6. Cleans and creates destination directories
7. Copies the processed files to both the RAID agency app and static app directories
8. Creates an index.ts file for each destination to export all models

### Output Files
- All TypeScript model files in:
  - `raid-agency-app/src/generated/raid/`
  - `raid-agency-app-static/src/generated/raid/`

### Dependencies
- Docker
- OpenAPI generator
- bash utilities (find, sed, awk)

## generate-references-config.json

### Purpose
Configuration file for the generate-references.sh script that defines language codes, output directories, and tables/columns to ignore.

### Content
- `language_codes`: List of ISO language codes to include in the language references
- `final_dir`: The destination directory for the reference files
- `ignore_tables`: Tables and columns to ignore during reference generation

## generate-references.sh

### Purpose
Extracts reference data from the database and saves it as JSON files for use in the RAID agency app.

### Usage
```bash
cd scripts
./generate-references.sh
```

### Process
1. Verifies the script is run from the correct directory
2. Loads environment variables for database connection
3. Reads configuration from generate-references-config.json
4. Creates a temporary output directory
5. Processes database tables based on schema_id and status columns
6. Optionally performs post-processing on specific files (e.g., subject_type.json)
7. Copies the generated reference files to the final destination

### Output Files
JSON files for each processed table in:
- `raid-agency-app/src/references/`

### Dependencies
- PostgreSQL client (psql)
- jq (JSON processor)
- Database connection details (via environment variables)

### Environment Variables
- `PG_USER`: Database username
- `PG_PASS`: Database password
- `PG_HOST`: Database host
- `PG_PORT`: Database port (defaults to 5432)
- `PG_DB`: Database name

## generate-subject-mapping.sh

### Purpose
Fetches and processes subject mapping data from ARDC vocabularies for use in both the RAID agency app and static app.

### Usage
```bash
cd scripts
./generate-subject-mapping.sh
```

### Process
1. Verifies the script is run from the correct directory
2. Downloads concept tree data for subjects from the ARDC vocabulary API
3. Uses `jq` to process the JSON data, extracting subject type information
4. Saves the processed data to both the agency app and static app mapping directories

### Output Files
- `raid-agency-app/src/mapping/data/subject-mapping.json`
- `raid-agency-app-static/src/mapping/data/subject-mapping.json`

### Dependencies
- cURL
- jq (JSON processor)