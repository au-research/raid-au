See the [Changelog audience](#changelog-audience) section for info about 
 the expected audience and content of the changelog.

# 2.5.9
## API
* Add 'Acknowledgements' as description type

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
