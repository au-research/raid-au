// RAID-537: E2E tests for RAiD create with required fields
// Subtask of RAID-535
//
// Tests that a user can create a RAiD by filling in the required metadata
// blocks. The form requires: Title (text + type + language), Date (start date),
// Access (type + statement), and at least one Contributor (ORCID).
//
// We use Embargoed Access so that the access statement fields are rendered and
// can be filled via the UI. Open Access hides the statement field while the API
// still requires a non-empty statement.text — see RAID-537 comments for details.
//
// Local environment notes:
// - The API only accepts sandbox ORCID IDs (https://sandbox.orcid.org/...)

import { test, expect } from "@playwright/test";
import { RaidFormPage } from "../page-objects/RaidFormPage";
import { RaidListPage } from "../page-objects/RaidListPage";

import { TitleSection } from "../page-objects/sections/TitleSection";
import { DateSection } from "../page-objects/sections/DateSection";
import { AccessSection } from "../page-objects/sections/AccessSection";
import { ContributorSection } from "../page-objects/sections/ContributorSection";
import { waitForSnackbar, extractPrefixSuffixFromUrl, futureDate } from "../utils/wait-helpers";

// A unique title text used to identify the created RAiD across both tests.
// The timestamp ensures each test run creates a distinct record.
const TEST_TITLE = `E2E Create Test ${Date.now()}`;
const START_DATE = "2024-01-15";
// The local dev environment only accepts sandbox ORCID IDs.
// The mock server (port 1080) must have a HEAD expectation for this ID.
// Known valid mock ORCID IDs: 0009-0002-5128-5184, 0009-0005-9091-4416
// See api-svc/raid-api/docker-compose/mockserver/expectations.json
const TEST_ORCID = "https://sandbox.orcid.org/0009-0002-5128-5184";
// Embargo expiry must be a future date in YYYY-MM-DD format
const EMBARGO_EXPIRY = futureDate(12);
const ACCESS_STATEMENT = "Embargoed for e2e testing";
// Display label for Embargoed Access in the MUI select dropdown
const EMBARGOED_LABEL = "Embargoed Access";

// Run tests serially so the second test can use the RAiD created by the first.
// fullyParallel: true in playwright.config.ts means tests in different describe
// blocks run in parallel, but test.describe.serial forces sequential execution
// within this block AND runs both tests in the same worker (shared closure state).
test.describe.serial("Create RAiD with required fields", () => {
  // Share the prefix/suffix of the created RAiD between the two tests so the
  // second test can verify the record appears on the list page.
  let createdPrefix: string;
  let createdSuffix: string;

  test(
    "fills required fields and saves a new RAiD, then view page shows correct title",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      const titleSection = new TitleSection(page);
      const dateSection = new DateSection(page);
      const accessSection = new AccessSection(page);
      const contributorSection = new ContributorSection(page);

      // Navigate to the create page
      await formPage.goto("/raids/new");

      // --- Title ---
      // The form pre-fills index 0 with type=Primary and language=eng.
      // We only need to provide the title text; type and language are already set.
      await titleSection.fillText(0, TEST_TITLE);

      // --- Date ---
      // The form pre-fills today's date. We overwrite with a known value so the
      // assertion is deterministic.
      await dateSection.fillStartDate(START_DATE);

      // --- Access ---
      // Select Embargoed Access to make the statement and expiry fields visible in
      // the UI. The API requires a non-empty access statement regardless of type,
      // and for Embargoed Access it also requires an embargo expiry date.
      await accessSection.selectAccessType(EMBARGOED_LABEL);
      await accessSection.fillStatementText(ACCESS_STATEMENT);
      await accessSection.fillEmbargoExpiry(EMBARGO_EXPIRY);

      // --- Contributor ---
      // The validation schema requires at least one contributor with a valid
      // ORCID. The local dev environment only accepts sandbox ORCIDs. The data
      // generator pre-fills position and role, so we only need the ORCID ID.
      await contributorSection.addItem();
      await contributorSection.fillOrcidId(0, TEST_ORCID);

      // Save
      await formPage.save();

      // Wait for the app to navigate to /raids/{prefix}/{suffix}
      await formPage.waitForSuccessfulSave();

      // Capture the handle for the second test
      const url = page.url();
      [createdPrefix, createdSuffix] = extractPrefixSuffixFromUrl(url);

      // The success snackbar should appear
      await waitForSnackbar(page, "Raid created successfully");

      // The view page should display the title text we entered.
      // RaidDisplay renders titles via DisplayItem which renders the text as a
      // Typography component. We use exact match scoped to <p> elements to avoid
      // a strict-mode conflict with the raw JSON <pre> block that also contains
      // the title text.
      await expect(
        page.locator("p").filter({ hasText: TEST_TITLE }).first()
      ).toBeVisible({ timeout: 15000 });

      // Confirm we are on the view URL (not the create URL)
      await expect(page).toHaveURL(/\/raids\/[^/]+\/[^/]+$/);
      await expect(page).not.toHaveURL(/\/raids\/new/);
    }
  );

  test(
    "created RAiD appears in the list page",
    { tag: "@local" },
    async ({ page }) => {
      const listPage = new RaidListPage(page);
      await listPage.goto();

      // The DataGrid should contain a row with the test title text
      const row = await listPage.findRow(TEST_TITLE);
      await expect(row).toBeVisible({ timeout: 15000 });
    }
  );
});
