# Data Fetching Scripts

This directory contains Node.js modules used to fetch RAiD data from APIs, enrich it with citations, and process it for use in the static site.

## Overview

The scripts in this directory are responsible for:
1. Authenticating with the RAiD API using OAuth2 client credentials
2. Automatically refreshing tokens so long-running scripts never send an expired token
3. Fetching all public and embargoed RAiD data
4. Enriching RAiD data with citations, ORCID names, ROR organisation names, and service point names
5. Fetching handles from multiple environments
6. Implementing smart caching to optimise performance
7. Saving processed data for the build process

## Architecture

The system is built using modular Node.js ES modules:
- **loadAppConfig.js** - Shared config loader (reads `public/app-config.json`, falls back to env vars)
- **tokenManager.js** - OAuth token lifecycle manager (auto-refreshes before expiry)
- **fetch-raids.js** - Main orchestration module
- **fetch-embargoed-raids.js** - Fetches embargoed RAiD summaries via a separate dumper client
- **fetch-citation.js** - Citation fetching and caching module
- **fetch-orcidData.js** - ORCID contributor name enrichment
- **fetch-ror.js** - ROR organisation name enrichment
- **fetch-sp.js** - Service point name enrichment
- **fetch-handles.js** - Multi-environment handle fetching module
- **apiCache.js** - Generic file-backed cache utility (used by ORCID and ROR modules)

## Scripts

### fetch-raids.js

**Purpose:** Main module that orchestrates the entire data fetching process.

**Features:**
- Automatic token refresh — tokens are re-fetched before expiry so scripts running 30+ minutes never fail with an expired token
- Concurrent DOI processing with configurable batch sizes
- HTML response detection — rejects doi.org HTML error pages so they are never cached as citations
- Smart caching for citations, ORCID, ROR, and service points with TTL
- Progress tracking with real-time updates
- Retry logic with exponential backoff
- Cross-platform compatibility (Windows/Mac/Linux)

**Flow:**
1. Loads config from `public/app-config.json` via `loadAppConfig.js` (falls back to env vars)
2. Validates required configuration
3. Authenticates with IAM using OAuth2 client credentials; token managed by `TokenManager`
4. Fetches all public RAiD data from the API
5. Enriches data with DOI citations, ORCID contributor names, ROR organisation names, and service point names
6. Saves enriched RAiD data to `src/raw-data/raids.json`
7. Fetches embargoed RAiD summaries using the dumper client
8. Extracts unique handles and saves them to `src/raw-data/handles.json`

**Requirements:**
- Node.js v14+ with ES module support
- `.env` file with required environment variables (see Configuration section)

**Output Files:**
- `src/raw-data/raids.json` - Enhanced RAiD data with citations
- `src/raw-data/handles.json` - Unique handles from all environments
- `src/raw-data/all-handles.json` - Combined handles from all environments
- `src/raw-data/.citation-cache.json` - Citation cache (if caching enabled)

**Usage:**
```bash
node scripts/fetch-raids.js
```

### fetch-citation.js

**Purpose:** Dedicated module for fetching and managing citations.

**Features:**
- Fetches citations in APA format from DOI.org
- Automatic fallback mechanisms for unsupported DOIs
- Built-in citation text cleaning and formatting
- In-memory caching with persistence
- Batch processing support
- Statistics tracking

**Exported Functions:**
- `fetchCitation({ doi, makeRequestWithRetry, stats, citationCache, config })` - Fetch a single citation

### fetch-handles.js

**Purpose:** Fetches and combines handles from multiple RAiD environments.

**Features:**
- Parallel fetching from all environments
- Graceful handling of environment failures
- Automatic deduplication
- Progress reporting per environment

**Default Environments:**
- Production: `https://static.prod.raid.org.au/api/handles.json`
- Stage: `https://static.stage.raid.org.au/api/handles.json`
- Demo: `https://static.demo.raid.org.au/api/handles.json`
- Test: `https://static.test.raid.org.au/api/handles.json`

## Configuration

Config is loaded by `scripts/loadAppConfig.js`, which reads `public/app-config.json` first and falls back to environment variables. This means you can configure the scripts with a JSON file, env vars, or both.

### `public/app-config.json` (required non-secret values)

```json
{
  "apiEndpoint": "https://app.your-env.raid.org.au",
  "iamEndpoint": "https://iam.your-env.raid.org.au",
  "iamClientId": "your-client-id",
  "raidDumperClientId": "your-dumper-client-id",
  "raidEnv": "prod",
  "caching": { "enabled": true, "ttlMs": 432000000 }
}
```

Copy `app-config.template.json` from the project root as a starting point.

### Secrets (environment variables only — never in the JSON file)

```env
IAM_CLIENT_SECRET=your-client-secret
RAID_DUMPER_CLIENT_SECRET=your-dumper-secret
```

### Optional performance tuning (environment variables)

```env
DATA_DIR=./src/raw-data        # Output directory (default: ./src/raw-data)
CONCURRENT_DOI_REQUESTS=5      # Parallel DOI requests (default: 5)
DOI_REQUEST_DELAY=100          # Delay between batches in ms (default: 100)
REQUEST_TIMEOUT=30000          # HTTP timeout in ms (default: 30000)
MAX_RETRIES=3                  # Maximum retry attempts (default: 3)
ENABLE_CACHING=true            # Enable citation caching (default: false)
CACHING_TIME=432000000         # Cache TTL in ms (default: 5 days)
VERBOSE_LOGGING=false          # Enable detailed logging (default: false)
```

## Installation

1. **Install Node.js dependencies:**
   ```bash
   npm install
   ```

2. **Set up config:**
   ```bash
   cp app-config.template.json public/app-config.json
   # Edit public/app-config.json with your endpoints and settings

   cp .env.template .env
   # Edit .env with your secrets (IAM_CLIENT_SECRET, RAID_DUMPER_CLIENT_SECRET)
   ```

3. **Make scripts executable (optional):**
   ```bash
   chmod +x scripts/*.js
   ```

## Execution

### Manual Execution:
```bash
# Fetch all data with citations
node scripts/fetch-raids.js

# Or using npm scripts
npm run scripts
```

### Automatic Execution:
Scripts are automatically executed:
- Before development server starts (`npm run dev`)
- Before building the site (`npm run build`)

This happens through the `predev` and `prebuild` npm scripts defined in `package.json`.

## Troubleshooting

### Common Issues:

1. **Module Not Found Errors:**
   - Ensure all `.js` files are in the `scripts/` directory
   - Check that you're using Node.js v14+ with ES module support
   - Verify `"type": "module"` is in your `package.json`

2. **Authentication Failures:**
   - Verify `iamEndpoint` and `iamClientId` are set in `public/app-config.json`
   - Verify `IAM_CLIENT_SECRET` is set as an environment variable
   - Check IAM endpoint accessibility and client permissions

3. **Citation Fetch Failures:**
   - Some DOIs may not support bibliography format
   - Script automatically falls back to error message
   - Check verbose logging for specific errors

4. **Timeout Issues:**
   - Increase `REQUEST_TIMEOUT` as an environment variable
   - Reduce `CONCURRENT_DOI_REQUESTS` for slower connections
   - Check network connectivity

5. **Cache Issues:**
   - Delete `.citation-cache.json` to start fresh
   - Disable caching with `ENABLE_CACHING=false`
   - Check file permissions in output directory

### Debug Mode:
Enable verbose logging for detailed output:
```bash
VERBOSE_LOGGING=true node scripts/fetch-raids.js
```

