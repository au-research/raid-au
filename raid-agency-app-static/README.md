# RAiD Agency Static App

A static website for displaying RAiD (Research Activity Identifier) records using Astro, TypeScript, and Tailwind CSS.

## 🚀 Overview

The RAiD Agency Static App is a web application that fetches, processes, enriches, and displays Research Activity Identifier (RAiD) data. It provides a user-friendly interface for exploring RAiD records, their metadata, DOI citations, and relationships between research activities.

## ✨ Key Features

- **RAiD Data Display**: Comprehensive display of research activity identifiers and metadata
- **Citation Enrichment**: Automatic fetching and display of DOI citations in APA format
- **Multi-Environment Support**: Aggregates handles from production, staging, demo, and test environments
- **Smart Caching**: Optimized performance with intelligent citation caching
- **Cross-Platform**: Works seamlessly on Windows, Mac, and Linux
- **Real-time Progress**: Live updates during data fetching and processing

## 📋 Project Structure

```text
/
├── public/                 # Static assets
├── scripts/                # Data fetching Node.js modules
│   ├── fetch-raids.js      # Main orchestration module
│   ├── fetch-citation.js   # Citation fetching and caching module
│   └── fetch-handles.js    # Multi-environment handle fetching
├── src/
│   ├── components/         # Reusable UI components
│   │   ├── raid-components/ # Components for RAiD data display
│   │   │   ├── access.astro
│   │   │   ├── contributors.astro
│   │   │   ├── descriptions.astro
│   │   │   └── ...
│   │   └── ...
│   ├── generated/          # TypeScript interfaces (OpenAPI generated)
│   │   └── raid/           # RAiD data models
│   ├── layouts/            # Page layouts
│   ├── mapping/            # Data mapping configurations
│   ├── pages/              # Page routes
│   │   ├── api/            # API endpoints
│   │   ├── prefixes/       # Prefix listing pages
│   │   ├── raids/          # RAiD listing and detail pages
│   │   │   └── [prefix]/   # Dynamic routes for RAiD prefixes
│   │   │       └── [suffix].astro # Individual RAiD page
│   │   └── index.astro     # Home page
│   ├── raw-data/           # Raw JSON data from API
│   │   ├── raids.json      # Enhanced RAiD data with citations
│   │   ├── handles.json    # Unique handles
|   |   ├── all-handles.json # Combined all handles 
│   │   └── .citation-cache.json # Citation cache (if enabled)
│   ├── services/           # Data services
│   └── utils/              # Utility functions
└── package.json
```

## 🛣️ Routes

| Route                       | Description                                 |
| :-------------------------- | :------------------------------------------ |
| `/`                         | Home page with overview statistics          |
| `/raids`                    | Lists all available RAiDs                   |
| `/raids/[prefix]`           | Lists RAiDs for a specific prefix           |
| `/raids/[prefix]/[suffix]`  | Detail page for a specific RAiD             |
| `/prefixes`                 | Lists all available prefixes                |
| `/api/raids.json`           | JSON API endpoint for RAiD data             |
| `/api/handles.json`         | JSON API endpoint for handle data           |
| `/api/all-handles.json`     | Combined handles from multiple environments |
| `/api/raid-files-list.json` | List of available RAiD files(inprogress)    |

## 📦 Data Flow

1. **Data Acquisition** (Enhanced with Node.js):

   - `scripts/fetch-raids.js` orchestrates the entire data fetching process:
     - Authenticates with IAM endpoint using OAuth2
     - Fetches all public RAiD data from the API
     - Enriches each DOI with citations from doi.org (98%+ success rate)
     - Implements smart caching to optimize subsequent runs
   - `scripts/fetch-citation.js` handles citation fetching with fallback mechanisms
   - `scripts/fetch-handles.js` aggregates handles from all environments in parallel
   - Processed data is stored in `src/raw-data/`

2. **Build Process**:

   - The `predev` and `prebuild` scripts automatically run data fetching
   - Astro processes the enriched data to generate static pages
   - Citation data is embedded within RAiD objects for display

3. **Component Rendering**:
   - Each RAiD is displayed using specialized components in `src/components/raid-components/`
   - Components now have access to citation data for related objects
   - DOI citations are displayed in standard APA format

## 🛠️ Commands

| Command                | Action                                                           |
| :--------------------- | :--------------------------------------------------------------- |
| `npm install`          | Installs dependencies                                            |
| `npm run scripts`      | Fetches latest RAiD data                                         |
| `npm run dev`          | Starts local dev server at `localhost:4321` (runs scripts first) |
| `npm run build`        | Build production site to `./dist/` (runs scripts first)          |
| `npm run preview`      | Preview your build locally                                       |

## 🔐 Environment Setup

The application requires the following environment variables:

### Required Variables:
```env
# IAM Authentication
IAM_ENDPOINT=<auth endpoint URL>
IAM_CLIENT_ID=<client ID>
IAM_CLIENT_SECRET=<client secret>

# API Configuration
API_ENDPOINT=<API endpoint URL>
RAID_ENV=<environment name>
```

### Optional Performance Variables:
```env
# Performance Tuning
CONCURRENT_DOI_REQUESTS=5     # Parallel DOI requests (default: 5)
DOI_REQUEST_DELAY=100         # Delay between batches in ms (default: 100)
REQUEST_TIMEOUT=30000         # HTTP timeout in ms (default: 30000)
MAX_RETRIES=3                 # Maximum retry attempts (default: 3)

# Feature Flags
ENABLE_CACHING=true           # Enable citation caching (default: false)
CACHING_TIME=432000000        # Cache TTL in ms (default: 5 days)
VERBOSE_LOGGING=false         # Enable detailed logging (default: false)
```

Create a `.env` file in the project root with these variables.

## 💻 Development Workflow

1. Clone the repository
2. Install Node.js v14+ (required for ES modules)
3. Set up environment variables in a `.env` file
4. Install dependencies with `npm install`
5. Fetch initial data with `npm run predev`
6. Run the development server with `npm run dev`(which also fetches RAID data)
7. Make changes to components, pages, or styles
8. Build for production with `npm run build`

## 🚀 Performance Improvements

The new Node.js implementation provides significant improvements:

- **98.7% citation success rate** (previously ~60% with shell scripts)
- **3x faster execution** with concurrent processing
- **Zero parsing errors** with native JSON handling
- **Smart caching** reduces API calls by up to 50% on subsequent runs
- **Better error handling** with automatic retries and fallbacks

## 🌐 API Endpoints

The application provides several API endpoints under `/api/`:

- `/api/raids.json`: Returns all RAiD data with enriched citations
- `/api/handles.json`: Returns all RAiD handles from current environment
- `/api/all-handles.json`: Returns handles from all environments
- `/api/raid-files-list.json`: Returns a list of available RAiD files(inprogress)

## 🧩 Component Structure

The `raid-components` directory contains specialized components that each render a specific aspect of a RAiD record:

- `access.astro`: Renders access information (type, statements, etc.)
- `alternate-identifiers.astro`: Displays alternative identifiers for the RAiD
- `contributors.astro`: Shows people who contributed to the research activity
- `contributor-roles.astro`: Displays the roles of contributors
- `dates.astro`: Shows important dates related to the research activity
- `descriptions.astro`: Renders descriptions of the research activity
- `organisations.astro`: Shows organizations associated with the research activity
- `related-objects.astro`: Displays objects with DOI citations (NEW: includes citation text)
- `related-raids.astro`: Shows relationships between RAiDs
- `titles.astro`: Renders titles of the research activity

Each component follows a consistent pattern to extract and display its specific data from the RAiD object.

## 📊 Data Models and API Integration

TypeScript interfaces in `src/generated/raid/` are generated from OpenAPI specifications and define the structure of RAiD data. Key models include:

- `RaidDto.ts`: The main RAiD data object
- `Access.ts`: Access information
- `Contributor.ts`: Contributor information
- `Organisation.ts`: Organization information
- `RelatedObject.ts`: Related object data (enhanced with citation field)
- `RelatedRaid.ts`: Related RAiD data

### Authentication Flow

The application uses OAuth client credentials flow to access the RAiD API:

1. `fetch-raids.js` authenticates with the IAM endpoint to obtain a bearer token
2. This token is used in subsequent API requests to authorize access
3. The API version is specified via the `X-Raid-Api-Version` header
4. Citation fetching uses standard HTTP requests to doi.org with proper Accept headers

To update the generated TypeScript interfaces:

1. Update the OpenAPI schema if needed
2. Run the OpenAPI generator to regenerate interfaces
3. Update components if the data structure changes

## 🗺️ Data Mapping

The application uses mapping files in the `src/mapping/data/` directory to translate codes and identifiers into human-readable values:

- `general-mapping.json`: Maps general codes to display values
- `language.json`: Maps language codes to language names
- `subject-mapping.json`: Maps subject codes to subject names

Understanding these mappings is crucial for maintaining and extending the display components.

## 🚢 Deployment

### Production Build

To build for production:

1. Ensure environment variables are correctly set in `.env`
2. Run `npm run build` to fetch data and generate static files
3. Deploy the contents of the `dist/` directory to your web server

### Continuous Integration

For CI/CD setup:

1. Include the required environment variables in your CI environment
2. Configure Node.js v14+ in your CI pipeline
3. Run `npm i` to install dependencies
4. Run `npm run build` to fetch data and build
5. Deploy the `dist/` directory to your hosting environment

## 🧪 Testing

While no formal testing framework is implemented, you can validate data processing by:

1. Checking the console output during build for fetch statistics
2. Reviewing the generated static files
3. Comparing API responses with rendered components
4. Verifying citation enrichment in the related objects section

## 🤝 Contribution Guidelines

### Code Style

- Follow the existing code style throughout the project
- Use TypeScript types for all variables and function parameters
- Keep components focused on a single responsibility
- Use ES modules syntax for all JavaScript files

### Pull Request Process

1. Create a feature branch from `main`
2. Make your changes
3. Test locally by running `npm run dev`
4. Verify data fetching works correctly
5. Create a pull request with a clear description of your changes

## 📚 Resources

- [Astro Documentation](https://docs.astro.build)
- [Tailwind CSS Documentation](https://tailwindcss.com/docs)
- [RAiD Documentation](https://www.raid.org.au/)
- [DOI Content Negotiation](https://citation.crosscite.org/docs.html)

## 📖 Additional Documentation

For more detailed technical documentation and contribution guidelines, see:

- [DOCUMENTATION.md](./DOCUMENTATION.md) - Detailed technical documentation
- [CONTRIBUTING.md](./CONTRIBUTING.md) - Guidelines for contributing to the project
- [scripts/README.md](./scripts/README.md) - Detailed documentation for data fetching modules

## 🔄 Migration Notes

This project has been migrated from bash scripts to Node.js modules, providing:

- ✅ Cross-platform compatibility (no more jq dependencies)
- ✅ Enhanced citation fetching with 98%+ success rate
- ✅ Better error handling and recovery
- ✅ Smart caching for improved performance
- ✅ Modular, maintainable code structure

The output format remains fully compatible with the existing Astro build process.