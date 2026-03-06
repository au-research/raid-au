# RAID-538: E2E Tests for Optional Metadata Blocks

## What changed and why

Added `e2e/tests/03-optional-metadata.spec.ts` — a Playwright end-to-end test that creates a RAiD via the UI with as many optional metadata sections populated as possible, then verifies the view page displays the saved data correctly.

The test was requested as a subtask of RAID-535 (E2E test coverage for the RAiD create flow) to complement the required-fields test in `02-create-raid.spec.ts`.

### Sections tested

- Title (required, filled via `TitleSection`)
- Date (required, filled via `DateSection`)
- Access — Embargoed (required; Embargoed used so statement + expiry fields are visible)
- Contributors — two contributors with distinct sandbox ORCID IDs
- Description — text, type (Primary), language (English via LanguageSelector Autocomplete)
- Alternate Identifier — id + free-text type
- Alternate URL
- Related Object — DOI + type (Dataset) + pre-populated category (Output)
- Related RAiD — handle + type (HasPart)
- Spatial Coverage — OpenStreetMap URI + place name ("Australia")

### Sections skipped (with TODO comments in the spec)

- Organisation — ROR lookup calls external `api.ror.org`; not available in local mock environment
- Subject — tree-view widget requires loaded subject code data; needs verification before enabling
- Traditional Knowledge — no UI section implemented yet

## Key implementation decisions

- `test.describe.serial(...)` used to share the created RAiD handle (prefix + suffix) between the create test and the view test.
- All view-page assertions scoped to `page.locator("p").filter({ hasText: value }).first()` to avoid strict-mode violations caused by the raw JSON `<pre>` block also containing the same text.
- Breadcrumb wait uses `page.getByLabel("breadcrumb")` because the element is `<div aria-label="breadcrumb">`, not a `<nav>`.
- `spatialCoverage.id` must match `^https://(www\.)?openstreetmap.org/.*$` — GeoNames URIs fail API validation.
- Related Object category is pre-populated by `relatedObjectDataGenerator()` when "Add Related Object" is clicked — no "Add Category" button click needed.
- `CheckboxField` does not set an HTML `id`; `checkLeader`/`checkContact` page object methods do not work in the DOM. The data generator pre-fills `leader: true` / `contact: true` for contributor 0, so no UI interaction is required.

## JIRA tickets

- Parent: [RAID-535](https://ardc.atlassian.net/browse/RAID-535)
- This subtask: [RAID-538](https://ardc.atlassian.net/browse/RAID-538)

## Files changed

- `/Users/robleney/workspace/raid-au/raid-agency-app/e2e/tests/03-optional-metadata.spec.ts` — new file
