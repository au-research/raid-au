# CLAUDE.md - RAiD Registration Agency

## Project Overview

RAiD (Research Activity Identifier) Registration Agency system. Multi-module Gradle project with a Java/Spring Boot API, React TypeScript frontend, and Astro static site.

## Project Structure

```
raid-au/
├── api-svc/              # Backend API
│   ├── raid-api/         # Spring Boot application (Java 17)
│   ├── db/               # Flyway migrations & JOOQ code generation
│   ├── idl-raid-v2/      # OpenAPI 3.0 spec & generated code
│   └── testFixtures/     # Shared test fixtures
├── raid-agency-app/      # React frontend (TypeScript, Vite, MUI)
├── raid-agency-app-static/ # Public static site (Astro, Tailwind)
├── iam/                  # Keycloak SPIs (Java)
├── sso/                  # SATOSA SAML proxy config
├── doc/                  # Documentation & ADRs
└── scripts/              # Operational scripts
```

## Build & Test Commands

### Backend (from project root)

```bash
# Build everything (compiles, runs unit tests)
./gradlew build

# Run unit tests only
./gradlew test

# Run integration tests (requires Docker)
./gradlew intTest

# Start local dev stack (Postgres + Keycloak via Docker, then API)
./gradlew dockerComposeUp bootRun -Dspring.profiles.active=dev

# Database migrations
./gradlew :api-svc:db:flywayMigrate

# Regenerate JOOQ classes after migration changes
./gradlew :api-svc:db:generateJooq
```

### Frontend - raid-agency-app (from `raid-agency-app/`)

```bash
yarn install              # Install dependencies
yarn build                # TypeScript check + Vite build (tsc && vite build)
yarn test                 # Unit tests (Vitest)
yarn dev                  # Dev server on port 7080
yarn e2e                  # Playwright E2E tests
yarn lint                 # ESLint
```

### Frontend - raid-agency-app-static (from `raid-agency-app-static/`)

```bash
yarn install
yarn build                # Fetch data + Astro check + build
yarn dev                  # Dev server (fetches data first)
```

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.4.x, PostgreSQL, JOOQ, Flyway, Keycloak 26.x
- **Frontend**: React 18, TypeScript 5.3, Vite, Material-UI 5, TanStack Query, React Hook Form + Zod
- **Static site**: Astro 5, TypeScript, Tailwind CSS
- **Package manager**: Yarn 1.22 (frontend apps)
- **Testing**: JUnit 5 + Mockito (backend), Vitest + Playwright (frontend)
- **Auth**: Keycloak (OAuth2/OIDC), SATOSA (SAML/eduGAIN federation)
- **External integrations**: ROR, ORCID, ISNI, DataCite, GeoNames

## Conventions

- **Commit messages**: `RAID-XXX: imperative description` (50/72 format)
- **Branches**: `feature/RAID-XXX` from `main`
- **Comment tags**: `TODO:INITIALS` (must fix), `SECURITY:XXX` (must resolve before release), `IMPROVE:` (nice to have)
- **SQL migrations**: `V<number>__description.sql` in `api-svc/db/src/main/resources/db/migration/`
- **API spec**: `api-svc/idl-raid-v2/src/raido-openapi-3.0.yaml` (generates controllers and models)
- **JOOQ generated code**: `api-svc/db/src/main/generated-jooq/` - do not edit manually

## Architecture Notes

- Backend is layered: Controller → Service → Repository (JOOQ, not JPA)
- Frontend uses path alias `@/` → `./src/`
- Frontend has generated TypeScript types from OpenAPI in `src/generated/`
- Database schema is `api_svc`, managed by Flyway with baseline at V25
- Schema changes must be backward-compatible (support rolling deployments)
- API changes to stable endpoints must be backward-compatible; use versioning for breaking changes

## Local Development

- **Postgres**: port 7432 (via Docker Compose)
- **Keycloak**: port 8001 (admin/admin), realm "raid"
- **API**: port 8080
- **Frontend**: port 7080
- **Test users**: `raid-test-user` / `password`, `uq-test-user` / `password`
- **External config**: `~/.config/raido/api-svc.gradle` and `api-svc-db.gradle`

## CI/CD

GitHub Actions workflows in `.github/workflows/`:
- `api-svc.yml` - Java build with JDK 17 (`./gradlew build`)
- `raid-agency-app.yml` - Node.js 20 build (`npm ci && npm run build && npm test`)
