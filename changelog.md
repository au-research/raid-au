See the [Changelog audience](#changelog-audience) section for info about
 the expected audience and content of the changelog.

# 2.14.0

## API
* Withdrew the `traditionalKnowledgeLabel` field pending co-design with Indigenous communities.
  The field has been removed from the RAiD schema and API specifications. Mint and update requests
  that include `traditionalKnowledgeLabel` are now rejected with a `400 Bad Request` and a standard
  validation error, instead of being silently accepted as before. The field was never rendered or
  persisted, so no existing data is affected; the underlying database tables are retained (PR #568).

# 2.13.0

> Note: This release scopes the service point admin role to a single service point but keeps the
> existing flat behaviour as a fallback, so there is no functional change for users yet. After
> deploying to test, stage or prod, an operator must run the one-off backfill endpoint (see IAM
> below) to grant the new scoped roles.

## IAM
* Introduced scoped service point admin roles. A user's admin authority is now scoped to a single
  service point, replacing the previous role that made a user an admin of every service point they
  belonged to. The scoped role is created, granted and revoked alongside the existing role, so
  behaviour is unchanged while both are maintained. A fallback flag keeps the previous behaviour in
  effect until the app consumes the scoped roles (PRs #551, #553).
* Added an operator-only, idempotent backfill endpoint,
  `POST /realms/raid/group/migrate-service-point-admins`, that grants the scoped roles to existing
  group admins. It is safe to re-run (PR #556).
* Seeded scoped roles for the dev-realm groups and added role-permissions documentation (PR #552).

## API
* Added support for the scoped service point admin authorisation model. The change is additive and
  not yet wired into any endpoint, so there is no behaviour change yet (PR #554).

# 2.12.0

## API
* RAiD permissions are now resolved on demand instead of being carried in the access token. The
  permissions API returns the caller's `userRaids` and `adminRaids` as lists, replacing the previous
  per-RAiD boolean claim. This stops the access token growing with each RAiD a user administers,
  which had been pushing request headers past the server's size limit (RAID-617, PR #550).
* The `raid-dumper` role can now read `GET /service-point/` (RAID-733, PR #561).

## App-client UI
* Reworked the service point notification experience -- pending service point requests now surface
  in a drawer instead of a popover, with corrected labels and layout fixes (RAID-403, PR #549).

## IAM
* The `raid` realm no longer emits the `admin_raids` access-token claim. A new Keycloak SPI endpoint,
  `GET /permissions`, returns a user's `userRaids` and `adminRaids` from their stored attributes, so
  the token stays small regardless of how many RAiDs the user can administer (RAID-617, PR #550).

## Static Landing Pages
* Added caching with a TTL for ORCID, ROR and service point lookups during the static site data
  fetch, reducing repeated external API calls on rebuilds (RAID-709, PR #546).
* Reworked access-token handling in the data-fetch scripts with a dedicated token manager that
  refreshes the token before it expires, corrected the citation-failed and cache counts, and removed
  the legacy shell fetch scripts in favour of the Node scripts (RAID-710, PR #555).

# 2.11.2

## API
* `postToDatacite` endpoint now accepts a `RaidUpdateRequest` body and returns `204 No Content`,
  replacing the previous full `RaidDto` request/response (RAID-610, PR #545).

# 2.11.1

## API
* DataCite errors are no longer swallowed -- `DataciteService` re-throws `HttpClientErrorException`
  so upstream DataCite failures propagate to the caller instead of failing silently
  (RAID-609, PR #447).

## App-client UI
* Fixed service point create and update form validation, resolving a defect where the ROR field's
  React Hook Form state and local component state drifted out of sync (RAID-480, PR #542).
* ROR iDs are now validated and trimmed of surrounding whitespace on entry (PR #542).
* Service points with an empty or invalid group ID are now handled gracefully instead of erroring,
  and organisation role end dates default to the RAiD's end date (RAID-659, PR #535).
* Removed the end-date validation on contributor positions and organisation roles, and made the
  ORCID placeholder and helper text configurable via `app-config` (RAID-708, PR #537).

## Static Landing Pages
* JSON-LD now prefers the primary title type for the schema.org `name` field and includes all
  titles (RAID-707, PRs #541, #544).
* Related RAiD relationships are now mapped to schema.org properties in JSON-LD (RAID-706, PR #531).

# 2.11.0

## API
* Description `text` field now accepts line breaks -- changed the validation pattern from
  `^\s*\S.*$` to `^\s*\S[\s\S]*$` so that multi-line content is no longer rejected
  (RAID-704, PR #534).
* `metadata.created` is now immutable on update -- the API preserves the original creation
  timestamp instead of allowing it to be overwritten (RAID-690, PR #525).

## App-client UI
* ORCID integration uplift -- contributor widget now validates ORCID iDs and displays the
  resolved identity after lookup (RAID-676, PR #532).
* New date picker component integrated throughout the RAiD form, replacing raw text inputs
  (RAID-674, PR #521).
* Contributor start and end dates now default to the RAiD's own start and end dates.
* Corrected "Create API key" label to "Create API token".

# 2.10.1

> Note: 2.10.0 was never deployed to production. Its migration of `schemaUri` fields to typed
> enums (see below) rejected existing ANZSRC-FOR subject URIs stored with a trailing slash
> (`.../anzsrc-for/2020/`), causing HTTP 500s on the `/raid/` list endpoint. 2.10.1 carries all
> of the 2.10.0 changes plus the data fix below.

## API
* Normalised legacy ANZSRC-FOR subject `schemaUri` values to the canonical no-trailing-slash form
  (`https://linked.data.gov.au/def/anzsrc-for/2020`) required by the new typed enums, via a
  prod-only Flyway migration (V41.1). The same migration repairs subject IDs in `raid_history`
  that an earlier prod-only fix (V40.1) corrupted with a blind text replace.

# 2.10.0

## App-client UI
* Removed bulk upload limit — CSV/Excel imports are no longer capped at 100 items.
* Title and description now display on static landing pages.
* Localised date formatting on static landing pages.
* Runtime `app-config` support for static landing pages, replacing build-time configuration.
* Fixed a static pages rendering defect.

## API
* LinkML data model introduction — enum values for subjects, titles, descriptions, contributor
  roles/positions, access types, related object types/categories, and traditional knowledge labels
  are now generated from SPARQL queries against external vocabularies (ANZSRC, COAR, etc.) with
  retry logic and local caching.
* String `schemaUri` fields migrated to typed enums across all API models — replaces free-text
  URIs with OpenAPI-generated enum types for stronger compile-time validation.
* RDF content negotiation added — the `/raid/{prefix}/{suffix}` endpoint now serves Turtle,
  RDF/XML, N-Triples, and JSON-LD representations via `Accept` header negotiation.
* Fixed `access.statement` conditional validation — Jakarta `@NotNull` on the generated
  `Statement` model caused blanket rejection before the custom validator could apply conditional
  logic (required for Embargoed, optional for Open). Fixed by making the field optional in the
  LinkML data model and letting the custom `AccessValidator` enforce the rule.
* Unified Jakarta Bean Validation and custom `ValidationService` error responses — overrode
  `handleMethodArgumentNotValid` in the exception handler to return `ValidationFailureResponse`
  instead of Spring's default `ProblemDetail` format.
* `OrganisationRepository.findOrCreate` made race-safe with `INSERT ... ON CONFLICT` to prevent
  duplicate key violations under concurrent requests.
* Null-guard in `addAdminRaid` — throws `UserNotFoundException` when Keycloak returns a null
  user instead of propagating a NullPointerException.

# 2.9.2

## Database
* Made the API schema-configurable for branch deployments by decoupling JOOQ schema references
  and adding `currentSchema` to the datasource URL (RAID-652, RAID-661).
* Converted schema grant to a repeatable migration with existing object grants (RAID-661).
* Migrated all raid `schemaUri` values to `https://raid.org/` and cleaned up legacy
  `localcontexts.org` vocabulary references (RAID-549).

## Static Landing Pages
* Enriched JSON-LD metadata for RDA harvesting with `name` and `headline` fields, and filtered
  the sitemap to exclude embargoed RAiDs (RAID-619, RAID-644).
* Added `robots.txt` with sitemap reference (RAID-619).
* Upgraded to Astro 6 and migrated Tailwind to PostCSS (RAID-628).

## App-client UI
* Added embargoed RAiD view with locale-formatted expiry dates in ISO 8601 UTC.
* Implemented SEO improvements with `noindex` on embargoed RAiDs and accessibility
  enhancements.

## Infrastructure
* Verified and validated the end-to-end Build-Push-Deploy-V2 pipeline including Keycloak
  auto-configuration and IAM policy integration (RAID-646).

## Dependencies
* `astro` 5.x to 6.4.4 in `raid-agency-app-static`.
* Various minor dependency updates in `raid-agency-app` and `raid-agency-app-static`.

# 2.9.1

## Database
* Removed `OWNER TO postgres` and `GRANT` statements from the Flyway baseline (`B25__baseline.sql`)
  and `V25__move-token-table.sql` to make migration scripts agnostic to database ownership and
  permissions.
* Fixed `V37__seo_subject_data.sql` by removing erroneous `raido.` database prefix from all table
  references (PR #464).

## Dependencies
* Upgraded Flyway from 9.22.3 to 11.20.3.
* Added `flyway-database-postgresql` driver required by Flyway 11.

# 2.9.0

## App-client UI
* Bulk upload for related objects — new CSV/Excel import flow with a preview table, inline error
  highlighting, and duplicate detection before confirming the upload (RAID-561).
* 100-item validation guard on bulk upload to prevent oversized submissions (RAID-561).
* Related object fields now accept multiple IDs instead of a single Type/DOI value (RAID-563).
* Accordion panel for related objects with a close button to dismiss the bulk upload section when
  not in use (RAID-561).
* Fixed Excel empty-cell parsing and updated CSV sentinel markers (RAiD-621 / RAID-563).
* Categories / Type field now accepts a single value (was incorrectly requiring multiple values)
  (RAiD-642).
* Preview table now highlights all required missing fields; error messages indicate the specific
  failing field (RAiD-642 / RAiD-629).
* Duplicate validation errors now report the location of the duplicate (RAiD-629).
* Invalid file type upload now shows a clear error message (RAiD-629).
* Fixed a maximum update depth (infinite re-render) issue and two further validation bugs in
  related object submission (RAiD-629).
* ORCID validation now accepts sandbox URLs (`sandbox.orcid.org`) (RAID-569).
* Contributor email field removed from the frontend form (RAID-569).
* `web.archive.org` accepted as a valid related object URL (RAID-569).
* Title `endDate` field made optional in the bulk upload context (RAID-561).
* Footer contact email is now driven from `app-config.json` rather than being hardcoded (RAID-592).
* Invite feature can be enabled/disabled via runtime config; disabled by default (RAID-624).
* Contributors and Invite URLs are configurable via the runtime config file (RAID-624).
* Footer links are configurable via the Keycloak UI (RAID-624).
* Branding updated — all remaining references to "Raido" removed and replaced with "RAiD";
  new RAiD logo favicon replaces the old "R" icon (RAiD-420).

## API
* Corrected `SERVICE_POINT_ID` / JSONB metadata divergence that could cause inconsistent raid
  records (RAID-618).
* Contributor email field removed from the API (RAID-569).

## Runtime Config
* Combined branding override and runtime config into a single `app-config.json` file to reduce
  confusion and duplication (RAID-624).
* Config file is now loaded from an external S3 resource at startup; fixed an override bug where
  S3-sourced config was silently replaced (RAID-624).

## IAM
* Upgraded Keycloak from 26.6.1 to 26.6.2.

## Documentation
* Major documentation overhaul — added Mermaid architecture diagram to `api-svc` README,
  replaced CNRI/APIDS handle docs with DataCite DOI minting docs, updated deployment,
  security, and technology-stack documentation, removed obsolete API architecture docs (RAID-547).
* Added sitemap investigation & proposal document (RAID-619).

## Dependencies
* `postcss` 8.5.8 → 8.5.12 in `raid-agency-app` (security patch).
* `brace-expansion` 1.1.12 → 1.1.14 in `raid-agency-app`.
* `astro` 5.18.1 → 6.1.8 in `raid-agency-app-static`.

# 2.8.5
## API
* Fix stale metadata column for raids with legacy github.com/au-research/raid-metadata vocabulary
  URIs — a Flyway migration nulls the stale entries and the existing backfill re-materialises them
  from the already-correct normalised tables on startup (RAID-575).
* Remove GET /raid/all endpoint — operator raid visibility is now handled by the existing
  findAllViewable query with proper access control (RAID-575).

# 2.8.4
## API
* Skip DataCite API calls for non-DOI handles — legacy handles (102.100.100/*, 10378.1/*) were
  never registered with DataCite and caused 500 errors on update (RAID-575).
* Allow operators to update raids across service points without a Keycloak group mapping (RAID-575).
* Add operator-only GET /raid/all endpoint to return all raids without RAID_HISTORY join limitation (RAID-575).
* Add web.archive.org as a valid related object schema URI (RAID-572).

## App-client UI
* Added web.archive.org as a valid related object with regex validation (RAID-572).
* Added Keycloak localization support for custom first-login messages (RAID-553).

## IAM
* Added Keycloak SPI for localization key-value pairs to support custom UI messages (RAID-553).
* Upgraded Keycloak from 26.5.7 to 26.6.0.

## Static Landing Pages
* Refactored service point fetch script to a single API call, replacing 12 individual batched
  requests that were failing due to token expiry (RAID-573).

## Scripts
* Added vocabulary URI uplift script to migrate legacy raids from github.com/au-research/raid-metadata
  URIs to vocabulary.raid.org / COAR equivalents (RAID-575).

# 2.8.3
* Dependency updates and Keycloak configuration fix. No user-facing changes.

# 2.8.2
## App-client UI
* Fixed the "Add Title" button flickering issue (RAiD-559).
* Fixed the RAiD logo size and corrected the Tasmania SVG path on static landing pages.
* Fixed the layout and Service Point rendering on static landing pages (RAiD-551).
* Added a Service Point name fetch module and fixed the URL rendering on the RAiD header
for static landing pages (RAiD-551).
* Added Service Point to the prefixes table on static landing pages (RAiD-551).
* Fixed type casting issues on static landing pages (RAiD-551).

# 2.8.1
## App-client UI
* Implemented automatic group rollback in Keycloak when Service Point creation fails after
  the group has already been created.
* Fixed the notification banner to display only in non-production environments.
* Added ARDC branding and mega menu to the static landing pages, with support for an
  external configuration file that can be updated without redeploying the application.

# 2.8.0
## API
* Fix NPE in IsniClient when personalName.nameUse is null
* Read raids from metadata column for faster list queries
* Materialise current RaidDto state with admin backfill endpoint
* Cache reference data repository lookups
* Parallelise I/O-bound validators in ValidationService
* Scope raid visibility to user's own service point and push filtering into SQL

## App-client UI
* Add ARDC branding with header, footer, NCRIS logo, dark/light mode, and mega-menu
* Scope raid visibility to user's own service point only
* Fix Access Type statement language validation issue
* Fix title language generation on load
* Add Playwright E2E test suite covering create, edit, validation, optional metadata, and vocabulary dropdowns

## IAM
* Add ARDC branding theme for Keycloak login page
* Upgrade Keycloak SPI dependency from 26.5.3 to 26.5.4

# 2.7.0
## API
* Add GET /raid/count endpoint for KPI reporting with service point and organisation breakdowns
* Add integration test for service point CRUD operations
* Remove DataCite repositoryId, prefix, and password from service point request models
* Pass DataCite values directly to factory instead of setting on request
* Remove unused RaidClient and RaidPermissionsDto

## App-client UI
* Add RAiD history page with API integration and route setup
* Add caching for ORCID and ROR data fetching on landing pages
* Display DataCite fields as read-only information from ServicePoint response
* Make access statement language field optional
* Make title language dropdown optional
* Fix defect related to saving the language in the Access field
* Fix access mapping subject code and organisation class typo
* Fix RAiD edit page defect and useQuery caching issue
* Move extended Citation types to src/model/raid/ - Raid App
* Add ORCID and ROR details to Raid data and render in the static pages as per the guidelines
* Move extended ROR and ORCID types to src/model/raid - static landing pages

## SSO
* Upgrade Keycloak from 24.0.1 to 26.5.3 (requires CDK deployment)
* Add unit test coverage for Keycloak SPI extensions
* Fix local dev Keycloak health check to use HTTPS
* Fix docker-compose health check for Keycloak 26

# 2.6.1
## App-client UI
* Bug fixes for Subject picker. Currently, supports ANZSRC-FOR and ANZSRC-SEO codes
* Login page customization now configurable via Keycloak User Interface
* Removed DataCite repository fields from Create Service Point page; fields remain available to view on Update Service Point page

# 2.6.0
## API
* Add endpoint to return all embargoed raids
* Add validation to prevent organisations from having simultaneous roles
* Add created and updated columns to service point db table

# 2.5.31
## API
* Support requests with or without trailing slashes in the URL

# 2.5.30
## App-client UI
* Implemented new UX/UI Subject picker. Currently, supports ANZSRC-FOR and ANZSRC-SEO codes

# 2.5.29
## App-client UI
* Turned off the contributors call until the new ORCID updater is ready

# 2.5.28
## API
* Update ORCID integration endpoints to use raid.org

# 2.5.27
## API
* Add SEO codes as subject types  

# 2.5.26
## API
* Fix bug in ISNI validation in PATCH requests

# 2.5.25
## API
* Add service point name to ORCID notification and change item type to membership in ORCID integration

# 2.5.23/2.5.24
## App-client UI
* Updated the date format to follow (YYYY-MM-DD) format throughout the App(View/Edit)
* Extracted root domain from current origin instead of hard-coding domain name

# 2.5.22
## SSO
* Update mappings from AAF to allow usernames to be shown in UI

# 2.5.21
## API
* Add validation to prevent duplicate organizations appearing in a RAiD

# 2.5.20
## API
* Bug fix in non-legacy endpoint

# 2.5.19
## API
* Add migration to remove null values from history
* Fix security config for non-legacy endpoint

# 2.5.18
> Note: This release jumps directly from 2.5.16 to 2.5.18 due to a deployment issue. There is no version 2.5.17.
* Migration to fix raidAgencyUrl field
* 
# 2.5.16
## App-client UI
* RAiD App Error handling: Added a generic message to the popup to display for the user instead of an empty popup with an unknown error in case of unexpected errors (400, 500, etc.)
* ROR Widget: Updated selected ROR display results on UI to show ror_display type instead of label type
* Contributors Get request Uri: Added a default case to render an empty uri value in case the instance doesn't contain any environment-specific keywords (demo, test, prod, etc.)

# 2.5.15
## API
* Check ORCIDs and ISNIs exist before sending to Datacite

# 2.5.14
## API
* Add names to creators when sending data to Datacite

# 2.5.13
> Note: This release jumps directly from 2.5.11 to 2.5.13 due to a deployment issue. There is no version 2.5.12.
## App-client UI
* RAiD App Notification Service update: Users with "Operator" permission can now be able to see pending requests from all the service points.
* View RAiD: RAiDs will no contributors can now see empty contributors block instead of "contributors not defined"

# 2.5.11
## API
* Add temporary endpoints to uplift legacy RAiDs and RAiDs with GitHub vocabularies
## App-client UI
* RAiD App Notification: Added new UX/UI for Notification Service
* Implemented notification service using React Context API for global state management, allowing any component to add or remove notifications without modifying core code
* Fixed defect related to hamburger menu toggle and click-away functionality to properly hide the drawer when clicking outside or selecting menu items
* Removed mandatory validation check for organisation field while minting a RAiD


# 2.5.10
## API
* Add 'AUTHENTICATION_REVOKED' contributor status for when ORCID owners remove permission for RAiD to update their record.
* Added POST endpoint for groupID creation
* Added support to validate the unique repository_Id constraint.

## ORCID Integration
* Set contributor status to 'AUTHENTICATION_REVOKED' if an update to ORCID record return a 401 status

## App-client UI
* Service Point Management: Added new UX/UI for service point creation and update functionality
* ROR Integration: Integrated Research Organization Registry (ROR) search widget
* Form Organization with Validation: Grouped form fields into logical sections (Service Point Owner, Data Cite Repository, Settings)
* Collapsible Interface: Added accordion component for form sections (default collapsed)
* Status Indicators: Added status indicators(loading/loaded/error) and graceful error handling on error scenarios.
* Defect Fix: Fixed the toggle buttons on both create and update service points forms.

# 2.5.9
## API
* Add 'Acknowledgements' as description type

## App-client UI
* Added attributes to the AAF-SAML identity provider in Keycloak to extract firstName, lastName, and email for displaying human-readable names in the UI(Service-points)
* Added 'Acknowledgements' as description type in the Web App.

# 2.5.8
## API
* Update scheme/host of `identifier.id` to 'https://raid.org' in all RAiDs 

## App-client UI
* Added missing subject('Public Health')-(HELP-2337).
* Contributors Management Fix - Resolved issue where users couldn't delete existing contributors and add new ones
* Removed Invitation feature due to quality concerns. It will return at a later date.
* Added Google Analytics tracking to the application.

# 2.5.7
## App-client UI
* Enhanced ROR Lookup UX: Redesigned Research Organization Registry lookup interface
* Uplifted search functionality with free text or RORID, visual status feedback (loading, success, error states)

## ORCID Integration
* Add dedicated authentication success page rather than redirecting to app.

# 2.5.6
## API
* Fix NullPointerException when adding/removing contributors

## App-client UI
* Resolved schemaUri validation issues affecting Subject field.
* Fixed validation handling for AccessType = Embargoed, ensuring correct schemaUri processing.

# 2.5.5
## ORCID Integration
* Bug fixes and refactoring

# 2.5.4
## App-client UI
* Implemented DOI citation fetching from DOI.org with "Accept: text/x-bibliography", supporting DataCite, Crossref, and mEDRA formats
* Added retry functionality for failed DOI citation requests to improve user experience
* Built Node.js modules to fetch RAiD and DOI citation data for static page generation using Astro framework(static website)
* Added caching mechanism to DOI citation functionality to store the citations for 5 days(configurable via .env file) in astro app(static website)
* Implemented markdown rendering on landing pages for rich text content(static website)
* Added comprehensive tooltip system across the entire RAiD application for improved user guidance
* Integrated DOI citation display within the main RAiD application
* Refactored React Hook Form implementation to properly handle controlled/uncontrolled component patterns and resolve console warnings
* Updated embargoed AccessTypes interface data structure for better data handling
* Updated ROR (Research Organization Registry) API routes to align with latest ROR.org changelog specifications
* Fixed edge cases where DOI citations were failing without proper error handling

# 2.5.3
## App-client UI
* Enhanced snackbar UX with notifications for successful RAiD minting and editing
* Improved ROR organization lookup with automatic name population
* Added dynamic error dialog to display API response errors
* Enhanced error handling with standardized error structure transformation
* Improved copy functionality with JSON data export from RAiD table
* Enhanced validation for DOI and ORCID identifiers to handle whitespace
* Improved alert dialog messages for required segments with sequence numbers
* Major project structure reorganization with API constants consolidation
* Enhanced end date validation for contributor positions and organization roles
* Various UI/UX improvements and bug fixes

## API
* Added endpoint to post RAiD data to Datacite with backfill capability
* Updated Datacite mapping to set 'RAiD' as resource type
* Enhanced service point input processing with whitespace trimming
* Improved security configuration and authorization handling
* Various security and data handling improvements

# 2.5.2
## App-client UI
* Fix refresh token request to prevent stale access token

# 2.5.0
## App-client UI
* Enhanced contributor management with ORCID integration
* Improved invite functionality with ORCID notifications
* Refactored form components for better maintainability
* Enhanced error handling and validation messages
* Improved loading states and error feedback
* Enhanced related RAiD components with validation for URLs
* Added cache manager for improved performance
* UI/UX improvements in forms and displays

## API
* Added contributor status and UUID fields
* Implemented ORCID integration for contributors
* Enhanced error handling in Datacite requests
* Improved validation for contributors and access statements
* Added more detailed logging
* Fixed RAID resource type in Datacite mapping

## Static Landing Pages
* Added JSON endpoints for raid data
* Improved CORS support for API endpoints
* Added related RAID tree visualization
* Enhanced data fetching with support for multiple service points

# 2.1.3
## API
• Update Datacite mapping to set resource type of RAiDs to 'project'

# 2.1.2
## App-client UI
* Component updates and refactoring
* Test framework and dependency management (Playwright for e2e, Vitest for unit tests)
* UI/UX enhancements (especially nested components)
* General maintenance and cleanup

# 2.1.1
## App-client UI
* Updated UI components and logic, including new mappings and service point UI logic.
* Removed unused code, such as mappings, helper functions, and Keycloak functions.
* Added new features like async/await, Postman request collection, SP switcher, CORS to user-groups endpoint, and a restore button.
* Created and refined error handling, history pages, and mapping files.

## API
* Fixed bug that set embargoed Raids to 'draft' in Datacite. Embargoed now use the 'register' event.
* Fixed bug with missing Raid history.

## IAM
* Added endpoint to allow users to switch between active service points.
* Added endpoint to expose all groups a user belongs to.

## BFF
* Added BFF (Backend for Frontend) to store additional data for UI that we don't want in the API.

## Infrastructure
* Added `stage` environment

# 2.1.0
## App-client UI
* Added API validation error messages to the frontend
* Added service point selector
* Added additional end-to-end tests
* Removed unused legacy code
 
## IAM
* SAML authentication with AAF 

## Infrastructure
# 2.0.1

_2024-05-14_

## App-client UI
* Minor bug fixes

## API
* Bug fix to allow RAiDs to be findable in Datacite

---
# 2.0.0

_2024-04-22_

## App-client UI
* All new UI

## API
* Handles replaced with DOIs
* RAiD versioning and history
* Removal of auth endpoints from API
* Removal of experimental API

## Infrastructure
* Shared stack templates across environments

---
# 1.2.1
* Fix to validation of ORCID to allow x in checksum
---

# 1.2

_2023-05-15_

## App-client UI
* Additions to mint/edit page:
  * Spatial coverage field
  * Traditional knowledge label field
* Option to disabled editing by service point

## API 
* PID validation
  * check ORCID exists for contributor
  * check ROR exists for organisation
  * check DOI exists for service point
  * note that the service-level-guide has been updated to allow that API 
    requests maybe rejected if they have too many PIDs associated
* New metadata blocks:
  * spatial coverages
  * traditional knowledge labels

## Infrastructure
* introduced the InMemory stubs for APIDS, ORCID, ROR and DOI
  * this is noted in changelog not because it changes anything, but the 
    deployment plan should take it into account, specifically: 
    * attention should be paid to env config (shouldn't need changing)
    * post-deploy tests should verify that we didn't accidentally start using 
      in-memory handles in production
* DB changes 
  * bumped width of `raid.handle` column
  * edit service point new column
* Metrics publishing to AWS CloudWatch introduced
  * must be enabled in config
  * remember to add the permissions to the ECS task role (not the ASG 
    cluster role), see DEMO `ApiSvcEcs`

---

# 1.1

_2023-04-11_

## App-client UI changes 

* Change public landing page title from "Raido" to "RAiD"
* Add sign-in warning of uBlock Origin / ORCID issue
* Add "Copy handle" button to the home page list
* Add download for "recently minted raids" report
* Default the lead organisation on the mint page to the institution
  associated with the service-point.
* Add item to menu for admin users to access API keys

## API changes

New metadata blocks:
* subjects 
* related Raids
* related objects 
* alternate identifiers


# 1.0

_2023-03-15_

Initial production deployment.

---

# Changelog audience

The changelog has multiple audiences:
* communications team - for preparing the notification email that
  will be sent for a new deployment
  * mostly interested in the UI section
  * but they also need to know about deprecations of stable API endpoints
* client API integrators
  * mostly interested in the API section
* test team
  * interested in both UI And API sections
* deployment team
  * interested in the infrastructure section
