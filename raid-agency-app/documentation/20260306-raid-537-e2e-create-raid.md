# RAID-537: E2E tests for RAiD create with required fields

## What changed and why

Added a new Playwright E2E test file `e2e/tests/02-create-raid.spec.ts` covering the RAiD creation flow.

The tests verify that a user can:
1. Navigate to `/raids/new`, fill all required form fields (title, start date, access type + statement, contributor ORCID), save successfully, and land on the view page showing the correct title.
2. Find the newly created RAiD in the list page at `/raids`.

### Key decisions

**Embargoed Access instead of Open Access**: The access statement text field is only rendered in the DOM when Embargoed Access is selected (`accessTypeId.includes("c_f1cf/")`). Open Access hides the statement field in the UI but the API still requires a non-empty `access.statement.text`. Using Embargoed Access makes the flow testable entirely through the UI.

**Mock ORCID IDs**: The local dev API profile routes ORCID existence checks to MockServer on port 1080. Only ORCID IDs with registered HEAD expectations succeed. Valid IDs: `0009-0002-5128-5184` and `0009-0005-9091-4416` (see `api-svc/raid-api/docker-compose/mockserver/expectations.json`).

**`test.describe.serial`**: The second test verifies the created RAiD appears in the list and reuses the handle extracted by the first test. `fullyParallel: true` in `playwright.config.ts` would run tests in separate workers (losing shared closure state), so `test.describe.serial` forces both tests into a single worker.

**Title locator**: The view page renders the title in both a `<p>` Typography element and a `<pre>` raw JSON block. `getByText(title)` triggers a strict-mode violation. The fix uses `page.locator("p").filter({ hasText: title }).first()`.

**`waitForURL` pattern**: `RaidFormPage.waitForSuccessfulSave()` uses `/\/raids\/\d+\/\d+$/` which does not match dotted prefix handles like `10378.1/12345`. The test uses the broader `/\/raids\/[^/]+\/[^/]+$/` directly.

## JIRA tickets

- Parent: [RAID-535](https://ardc.atlassian.net/browse/RAID-535)
- This ticket: [RAID-537](https://ardc.atlassian.net/browse/RAID-537)

## Files changed

- `e2e/tests/02-create-raid.spec.ts` — new test file
