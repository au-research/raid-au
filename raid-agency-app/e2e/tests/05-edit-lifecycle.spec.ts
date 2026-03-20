// RAID-540: E2E tests for RAiD edit lifecycle
// Subtask of RAID-535
//
// Tests the full create → edit → verify lifecycle:
//   1. Create a RAiD with required fields and capture the handle
//   2. Navigate to the edit page, modify fields (title, description, start date),
//      save, and verify the view page reflects the updated values
//   3. Verify the list page shows the updated title
//
// Local environment notes:
//   - API only accepts sandbox ORCID IDs (https://sandbox.orcid.org/...)
//   - Mock ORCID IDs: 0009-0002-5128-5184, 0009-0005-9091-4416
//   - Embargo access used to keep access statement fields visible in the UI
//   - Edit page URL: /raids/{prefix}/{suffix}/edit
//   - Success snackbar text after edit: "Raid updated successfully"

import { test, expect } from "@playwright/test";
import { RaidFormPage } from "../page-objects/RaidFormPage";
import { RaidListPage } from "../page-objects/RaidListPage";

import { TitleSection } from "../page-objects/sections/TitleSection";
import { DateSection } from "../page-objects/sections/DateSection";
import { AccessSection } from "../page-objects/sections/AccessSection";
import { ContributorSection } from "../page-objects/sections/ContributorSection";
import { DescriptionSection } from "../page-objects/sections/DescriptionSection";
import {
  waitForSnackbar,
  extractPrefixSuffixFromUrl,
  futureDate,
} from "../utils/wait-helpers";

// Unique timestamps so each test run creates distinct records
const RUN_ID = Date.now();

// Original values used when the RAiD is first created
const ORIGINAL_TITLE = `E2E Edit Lifecycle Original ${RUN_ID}`;
const ORIGINAL_START_DATE = "2024-01-15";

// Updated values applied during the edit step
const UPDATED_TITLE = `E2E Edit Lifecycle Updated ${RUN_ID}`;
const UPDATED_START_DATE = "2024-06-01";
const ADDED_DESCRIPTION =
  "Description added by the e2e edit lifecycle test.";

// Required access fields — Embargoed so the statement text field is visible
const EMBARGOED_LABEL = "Embargoed Access";
const ACCESS_STATEMENT = "Embargoed for e2e edit lifecycle testing";
const EMBARGO_EXPIRY = futureDate(12);

// Contributor ORCID accepted by the local mock server
const TEST_ORCID = "https://sandbox.orcid.org/0009-0002-5128-5184";

// Run all tests serially so they share the closure state (prefix/suffix)
// and execute in the correct order: create → edit → verify
test.describe.serial("RAiD edit lifecycle", () => {
  let createdPrefix: string;
  let createdSuffix: string;

  // -----------------------------------------------------------------------
  // Test 1: Create a RAiD with required fields
  // -----------------------------------------------------------------------
  test(
    "creates a RAiD with required fields and captures the handle",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      const titleSection = new TitleSection(page);
      const dateSection = new DateSection(page);
      const accessSection = new AccessSection(page);
      const contributorSection = new ContributorSection(page);

      await formPage.goto("/raids/new");

      // Title — the form pre-fills type=Primary and language=eng
      await titleSection.fillText(0, ORIGINAL_TITLE);

      // Date — overwrite the pre-filled today's date with a deterministic value
      await dateSection.fillStartDate(ORIGINAL_START_DATE);

      // Access — Embargoed so the statement and expiry fields are rendered
      await accessSection.selectAccessType(EMBARGOED_LABEL);
      await accessSection.fillStatementText(ACCESS_STATEMENT);
      await accessSection.fillEmbargoExpiry(EMBARGO_EXPIRY);

      // Contributor — at least one sandbox ORCID is required
      await contributorSection.addItem();
      await contributorSection.fillOrcidId(0, TEST_ORCID);

      await formPage.save();
      await formPage.waitForSuccessfulSave();

      // Capture prefix/suffix for subsequent tests
      const url = page.url();
      [createdPrefix, createdSuffix] = extractPrefixSuffixFromUrl(url);

      await waitForSnackbar(page, "Raid created successfully");

      // The view page renders title text in a Typography <p> element.
      // Scope to <p> to avoid a strict-mode conflict with the raw JSON <pre>
      // block that also contains the title text.
      await expect(
        page.locator("p").filter({ hasText: ORIGINAL_TITLE }).first()
      ).toBeVisible({ timeout: 15000 });

      await expect(page).toHaveURL(/\/raids\/[^/]+\/[^/]+$/);
      await expect(page).not.toHaveURL(/\/raids\/new/);
    }
  );

  // -----------------------------------------------------------------------
  // Test 2: Edit the RAiD — modify title, add description, change start date
  // -----------------------------------------------------------------------
  test(
    "edits the RAiD and verifies the view page shows updated values",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      const titleSection = new TitleSection(page);
      const dateSection = new DateSection(page);
      const descriptionSection = new DescriptionSection(page);

      // Navigate directly to the edit page using the handle from test 1
      await formPage.goto(`/raids/${createdPrefix}/${createdSuffix}/edit`);

      // Verify the form pre-populates with the original title before editing
      await expect(titleSection.locatorForText(0)).toHaveValue(
        ORIGINAL_TITLE,
        { timeout: 10000 }
      );

      // --- Update title ---
      await titleSection.fillText(0, UPDATED_TITLE);

      // --- Update start date ---
      await dateSection.fillStartDate(UPDATED_START_DATE);

      // --- Add a description (not present in the original create) ---
      await descriptionSection.addItem();
      await descriptionSection.fillText(0, ADDED_DESCRIPTION);
      await descriptionSection.selectType(0, "Primary");

      // Save the edit
      await formPage.save();

      // After a successful edit the app navigates back to the view page
      await formPage.waitForSuccessfulSave();

      await waitForSnackbar(page, "Raid updated successfully");

      // Verify the view page URL is the same RAiD (not the edit URL)
      await expect(page).toHaveURL(
        new RegExp(`/raids/${createdPrefix}/${createdSuffix}$`)
      );
      await expect(page).not.toHaveURL(/\/edit$/);

      // View page should show the UPDATED title
      await expect(
        page.locator("p").filter({ hasText: UPDATED_TITLE }).first()
      ).toBeVisible({ timeout: 15000 });

      // Both titles contain the same RUN_ID, so this guards against a partial-update
      // bug where the edit appends rather than replaces the original title entry.
      await expect(
        page.locator("p").filter({ hasText: ORIGINAL_TITLE })
      ).toHaveCount(0);

      // View page should show the added description
      await expect(
        page.locator("p").filter({ hasText: ADDED_DESCRIPTION }).first()
      ).toBeVisible({ timeout: 10000 });
    }
  );

  // -----------------------------------------------------------------------
  // Test 3: List page shows the updated title
  // -----------------------------------------------------------------------
  test(
    "list page shows the updated title after editing",
    { tag: "@local" },
    async ({ page }) => {
      const listPage = new RaidListPage(page);
      await listPage.goto();

      // The DataGrid row should use the updated title, not the original
      const row = await listPage.findRow(UPDATED_TITLE);
      await expect(row).toBeVisible({ timeout: 15000 });

      // The original title should not appear in the list
      const originalRow = page.getByRole("row").filter({ hasText: ORIGINAL_TITLE });
      await expect(originalRow).toHaveCount(0);
    }
  );
});
