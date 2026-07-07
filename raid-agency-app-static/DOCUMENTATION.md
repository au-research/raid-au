# RAiD Agency Static App Developer Documentation

This document provides technical documentation for developers working on the RAiD Agency Static App.

## Architecture Overview

The RAiD Agency Static App is built on the Astro framework, using a static site generation approach to create a lightweight, fast-loading application for browsing RAiD (Research Activity Identifier) data.

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│  Data Fetching  │────▶│  Static Site    │────▶│  Static HTML    │
│  (Node Modules) │     │  Generation     │     │  (User Browser) │
│                 │     │  (Astro)        │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### Key Components

1. **Data Fetching**: Node modules fetch data from the RAiD API
2. **Static Site Generation**: Astro transforms the data into static HTML
3. **Component Rendering**: Specialized components for each RAiD data type

## Detailed Technical Documentation

### Data Acquisition

The application fetches data using Node.js scripts in the `scripts/` directory:

- `fetch-raids.js`: Main orchestrator — authenticates, fetches all public RAiD data, and coordinates all enrichment steps
- `fetch-embargoed-raids.js`: Fetches embargoed RAiD summaries using a separate dumper client
- `fetch-citation.js`: Fetches APA citations from DOI.org with optional caching
- `fetch-orcidData.js`: Enriches contributor data with ORCID display names
- `fetch-ror.js`: Enriches organisation data with names from the ROR API
- `fetch-sp.js`: Fetches service point names from the RAiD API
- `fetch-handles.js`: Extracts unique handles from RAiD data
- `tokenManager.js`: Manages OAuth token lifecycle — automatically re-fetches tokens before expiry so long-running scripts never send an expired token

The data flow is as follows:

1. Load configuration from `public/app-config.json` and secrets from environment variables
2. Authenticate with IAM using OAuth 2.0 client credentials to obtain a bearer token
3. Fetch all public RAiD data from the API
4. Enrich each RAiD record with DOI citations, ORCID contributor names, ROR organisation names, and service point names
5. Fetch embargoed RAiD summaries using the dumper client credentials
6. Save all enriched data to `src/raw-data/` as JSON
7. JSON data parsed by Astro during the build process

### Data Processing

The Astro framework processes the data during build:

1. Data is loaded and parsed from JSON files
2. Routes are generated for each RAiD using `getStaticPaths()`
3. Data is passed to components for rendering
4. Static HTML is generated

### Components

Components are organized by functionality:

- **Structural Components**: Layout, navigation, headers
- **RAiD Components**: Specialized for different RAiD data types
- **Utility Components**: Reusable interface elements

Each raid-component follows this general pattern:

```typescript
// Example component pattern
export interface Props {
  raid: RaidDto;
}

const { raid } = Astro.props;
const data = extractRelevantData(raid);

function extractRelevantData(raid: RaidDto) {
  // Process and extract the specific data needed by this component
  return processedData;
}
```

### Data Models

The application uses TypeScript interfaces generated from OpenAPI specifications to ensure type safety:

- Interfaces define the structure of RAiD data
- Components are strongly typed using these interfaces
- Data transformations maintain type safety

### Routing

The application uses Astro's file-based routing system:

- Pages in `src/pages/` map to routes in the application
- Dynamic routes use the `[param]` naming convention
- Nested routes (like `/raids/[prefix]/[suffix]`) match RAiD identifier patterns

### API Integration

The application integrates with the RAiD API:

1. Authentication via OAuth 2.0 client credentials flow (machine-to-machine — no user session)
2. Tokens are managed by `TokenManager`, which automatically re-fetches a new token when the current one is within 60 seconds of expiry — this ensures long-running build scripts (30+ minutes) never fail due to an expired token
3. Bearer token used for authenticated API requests (`/raid/all-public`, `/service-point/`, `/raid/all-embargoed`)
4. Responses processed and transformed into site content

## Data Mapping System

The application uses a mapping system to translate codes and identifiers into human-readable values:

- `general-mapping.json`: General mappings for various codes
- `language.json`: Maps language codes to language names
- `subject-mapping.json`: Maps subject codes to subject names

To add a new mapping:

1. Add the mapping file to `src/mapping/data/`
2. Update the appropriate utility file to use the mapping
3. Use the mapping in components as needed

## Caching and Performance

The application uses several techniques to optimize performance:

1. Static site generation for fast loading
2. Data fetched at build time, not runtime
3. Minimal JavaScript in the client
4. Tailwind CSS for optimized styling

## Troubleshooting

### Common Issues

1. **Authentication Failures**: Check that `iamEndpoint`, `iamClientId` are set in `public/app-config.json` and `IAM_CLIENT_SECRET` is set as an environment variable
2. **Build Errors**: Check the data format and TypeScript errors
3. **Component Errors**: Verify component props and data structure

### Debugging Tips

- Check the script output for API errors (token acquisition, enrichment failures, cache stats)
- Inspect the JSON data files in `src/raw-data/` to verify correct format
- Use TypeScript to catch type errors during development
- Review Astro build logs for component rendering issues

## Extending the Application

### Adding a New RAiD Data Type

To add a new RAiD data type component:

1. Create a new component in `src/components/raid-components/`
2. Update TypeScript interfaces if needed
3. Add the component to the RAiD detail page template
4. Test with various data scenarios

### Adding a New Page

To add a new page:

1. Create a new `.astro` file in `src/pages/`
2. Import required components and data services
3. Implement the page layout and functionality
4. Update navigation components if needed

## API Reference

### Configuration

Non-secret configuration lives in `public/app-config.json`. This file is read by data-fetch scripts at build time and served as `/app-config.json` at runtime (for client-side features like Google Analytics).

| Field | Description |
|---|---|
| `apiEndpoint` | RAiD API base URL |
| `iamEndpoint` | Keycloak IAM base URL |
| `iamClientId` | OAuth client ID for public data fetching |
| `raidDumperClientId` | OAuth client ID for embargoed data |
| `raidEnv` | Environment name: `prod` \| `demo` \| `test` \| `dev` |
| `siteUrl` | Public URL the site is served from (used for sitemap) |
| `raidUrl` | Base URL for resolving RAiD handles |
| `analytics.gaMeasurementId` | Google Analytics ID for prod |
| `analytics.gaMeasurementIdDemo` | Google Analytics ID for demo |
| `caching.enabled` | Enable citation caching |
| `caching.ttlMs` | Cache TTL in milliseconds |
| `header` / `footer` | Branding — see `app-config.template.json` |

### Secrets (environment variables only)

Secrets are **never** stored in `app-config.json`.

| Variable | Description |
|---|---|
| `IAM_CLIENT_SECRET` | OAuth client secret for public data fetching |
| `RAID_DUMPER_CLIENT_SECRET` | OAuth client secret for embargoed data |

### Optional performance tuning (environment variables)

| Variable | Default | Description |
|---|---|---|
| `CONCURRENT_DOI_REQUESTS` | `5` | Parallel DOI requests |
| `DOI_REQUEST_DELAY` | `100` | Delay between DOI batches (ms) |
| `REQUEST_TIMEOUT` | `30000` | HTTP request timeout (ms) |
| `MAX_RETRIES` | `3` | Maximum retry attempts |
| `ENABLE_CACHING` | `false` | Enable citation caching |
| `CACHING_TIME` | `432000000` | Cache TTL (ms) |
| `VERBOSE_LOGGING` | `false` | Enable detailed logging |


### API Endpoints

The application provides several API endpoints:

- `/api/raids.json`: Returns all RAiD data
- `/api/handles.json`: Returns RAiD handles
- `/api/all-handles.json`: Returns handles from all environments
- `/api/raid-files-list.json`: Returns a list of available RAiD files
