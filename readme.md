# Raid Registration Agency

## Table of Contents
1. [Getting Started](#getting-started)
2. [Installation](#installation)
3. [Running the Application](#running-the-application)
4. [Running E2E Tests](#running-e2e-tests)
5. [Project Structure](#project-structure)
6. [API Documentation](#api-documentation)
7. [RAiD Search API](#raid-search-api)
8. [Contributing](#contributing)
9. [License](#license)
## Getting Started
To get started you'll need the following tools installed in your machine:
- Node.js >= 18
- Java >= 17
- Docker >= 27
## Installation
1. Clone the repository
```bash
git clone  https://github.com/au-research/raid-au.git
```
2. Build the API
```bash
cd raid-au
./gradlew build
```
3. Install dependencies for the app
```bash
cd raid-agency-app
npm install
```
## Running the Application
1. Start the local dev stack (Docker services + API)
```bash
./gradlew bootRunLocal
```
This starts PostgreSQL, Keycloak, and MockServer via Docker Compose, then launches the Spring Boot API with the `dev` profile. The API runs on `http://localhost:8080`.

2. Start the frontend (in a separate terminal)
```bash
cd raid-agency-app
npm run dev
```
The app runs on `http://localhost:7080`.

## Running E2E Tests
End-to-end tests use [Playwright](https://playwright.dev/) and require the local dev stack to be running.

1. Start the local dev stack (if not already running)
```bash
./gradlew bootRunLocal
```

2. Run the tests (in a separate terminal)
```bash
cd raid-agency-app
npx playwright test
```
## Project Structure
|                      |                                                                                                |
| -------------------- | ---------------------------------------------------------------------------------------------- |
| `api-svc/db`         | The database migrations (handled by flyway) and generated database classes (handled by JOOQ)   |
| `api-svc/idl-raid-v2`| OpenApi specs and code generation for API controllers                                          |
| `api-svc/raid-api`   | API Spring Boot application                                                                    | 
| `iam/realms`         | Keycloak configuration used in local environment/integration tests. Loaded at startup          |
| `iam/src`            | Keycloak SPIs to handle group/raid permissions.                                                |
| `sso/`               | Docker config for Satosa. This allows SAML authentication between AAF and Keycloak for eduGAIN |

## API Documentation
* [Swagger](https://api.demo.raid.org.au/swagger-ui/index.html#/raid/findRaidByName)
* [RAiD Metadata Schema](https://metadata.raid.org/en/latest/index.html)

## RAiD Search API

The RAiD Search API provides a publicly accessible endpoint for searching RAiDs via the Datacite GraphQL API. It is deployed as an AWS Lambda function behind API Gateway at `api.raid.org`.

### Endpoint

```
GET https://api.raid.org
```

### Query Parameters

Only one filter parameter may be used per request.

| Parameter       | Description                                        | Example                       |
| --------------- | -------------------------------------------------- | ----------------------------- |
| `contributor`   | Filter by contributor ORCID iD                     | `0000-0002-1825-0097`        |
| `organisation`  | Filter by organisation ROR ID                      | `038sjwq09`                  |
| `relatedObject` | Filter by related object DOI                       | `10.1234/example`            |
| `relatedRaid`   | Filter by related RAiD ID                          | `10.26312/db3c5429`          |
| `limit`         | Page size (default: 25, max: 100)                  | `25`                         |
| `cursor`        | Pagination cursor from a previous response          | `MTIzNDU2Nzg5MA==`           |

### Examples

List all RAiDs:
```bash
curl "https://api.raid.org"
```

Search by contributor ORCID:
```bash
curl "https://api.raid.org?contributor=0000-0002-1825-0097"
```

Search by organisation:
```bash
curl "https://api.raid.org?organisation=038sjwq09&limit=10"
```

Paginate through results:
```bash
curl "https://api.raid.org?organisation=038sjwq09&cursor=MTIzNDU2Nzg5MA=="
```

### Response

```json
{
  "data": [
    { "identifier": { "id": "https://raid.org/10.26312/db3c5429" } },
    { "identifier": { "id": "https://raid.org/10.26312/a1b2c3d4" } }
  ],
  "totalCount": 42,
  "nextCursor": "MTIzNDU2Nzg5MA==",
  "hasMore": true
}
```

When there are no more results, `nextCursor` is absent and `hasMore` is `false`.

# Contributing
We welcome contributions from the community! Whether you're fixing bugs, adding new features, improving documentation, or suggesting ideas, we'd love to have your input. Feel free to raise a pull request (include tests).
# License
This project is licensed under the Apache 2.0 License - see the license.txt file for details.
