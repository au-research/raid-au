// RAID-538: E2E tests for optional metadata blocks
// Subtask of RAID-535
//
// Tests that a user can create a RAiD by filling in all optional metadata
// blocks beyond the required fields. The test creates a single RAiD with as
// many optional sections populated as possible, then verifies the view page
// displays the saved data correctly.
//
// Sections tested:
//   - Description (text + type + language)
//   - Alternate Identifier (id + type)
//   - Alternate URL (url)
//   - Related Object (DOI + type + category)
//   - Related RAiD (handle + type)
//   - Spatial Coverage (URI + place name)
//
// Sections skipped with TODO:
//   - Organisation: The ROR lookup widget calls the external api.ror.org API,
//     which is not available in the local mock environment. TODO: add mock or
//     stub for the ROR search endpoint before enabling this section.
//   - Subject: The tree-view widget requires subject code data to be loaded
//     at runtime; the local environment needs verified subject codes before
//     this can be enabled reliably. TODO: verify available subject codes.
//   - Traditional Knowledge: Not yet implemented in the form (no UI section).
//   - Contributor extra roles/positions: The required-fields test (02) already
//     covers the contributor section. Additional contributors are tested via
//     the second contributor added in this test.
//
// Local environment notes:
//   - API only accepts sandbox ORCID IDs (https://sandbox.orcid.org/...)
//   - Mock ORCID IDs: 0009-0002-5128-5184, 0009-0005-9091-4416
//   - Embargo access used to keep access statement fields visible in the UI

import { test, expect } from "@playwright/test";
import { RaidFormPage } from "../page-objects/RaidFormPage";

import { TitleSection } from "../page-objects/sections/TitleSection";
import { DateSection } from "../page-objects/sections/DateSection";
import { AccessSection } from "../page-objects/sections/AccessSection";
import { ContributorSection } from "../page-objects/sections/ContributorSection";
import { DescriptionSection } from "../page-objects/sections/DescriptionSection";
import { AlternateIdentifierSection } from "../page-objects/sections/AlternateIdentifierSection";
import { AlternateUrlSection } from "../page-objects/sections/AlternateUrlSection";
import { RelatedObjectSection } from "../page-objects/sections/RelatedObjectSection";
import { RelatedRaidSection } from "../page-objects/sections/RelatedRaidSection";
import { SpatialCoverageSection } from "../page-objects/sections/SpatialCoverageSection";
import {
  waitForSnackbar,
  extractPrefixSuffixFromUrl,
  futureDate,
} from "../utils/wait-helpers";

// Unique title for this test run so the record is identifiable on the list page
const TEST_TITLE = `E2E Optional Metadata Test ${Date.now()}`;
const START_DATE = "2024-03-01";

// Required access fields — Embargoed so the statement text field is visible
const EMBARGOED_LABEL = "Embargoed Access";
const ACCESS_STATEMENT = "Embargoed for optional metadata e2e testing";
const EMBARGO_EXPIRY = futureDate(12);

// Contributor ORCID IDs accepted by the local mock server
const ORCID_PRIMARY = "https://sandbox.orcid.org/0009-0002-5128-5184";
const ORCID_SECONDARY = "https://sandbox.orcid.org/0009-0005-9091-4416";

// Description test data
const DESCRIPTION_TEXT =
  "This RAiD was created by an automated e2e test to verify optional metadata blocks.";
const DESCRIPTION_TYPE_LABEL = "Primary";

// Alternate identifier
const ALT_ID_VALUE = "e2e-optional-test-001";
const ALT_ID_TYPE = "local";

// Alternate URL
const ALT_URL = "https://example.com/e2e-optional-raid";

// Related object — use a known public DOI
const RELATED_OBJECT_DOI = "https://doi.org/10.5281/zenodo.1234567";
const RELATED_OBJECT_TYPE_LABEL = "Dataset";
const RELATED_OBJECT_CATEGORY_LABEL = "Output";

// Related RAiD — use a well-formed but non-existent handle so the form
// accepts it without making a successful external lookup
const RELATED_RAID_HANDLE = "https://raid.org/10378.1/999999";
const RELATED_RAID_TYPE_LABEL = "HasPart";

// Spatial Coverage — OpenStreetMap URI (the API requires ^https://(www\.)?openstreetmap.org/.*$)
const SPATIAL_COVERAGE_URI = "https://www.openstreetmap.org/relation/80500";
const SPATIAL_COVERAGE_PLACE = "Australia";

// Run tests serially: the first test creates the RAiD and captures its URL;
// the second test navigates to the view page and asserts on the saved data.
test.describe.serial("Create RAiD with optional metadata blocks", () => {
  let createdPrefix: string;
  let createdSuffix: string;

  test(
    "fills optional metadata sections and saves a new RAiD",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      const titleSection = new TitleSection(page);
      const dateSection = new DateSection(page);
      const accessSection = new AccessSection(page);
      const contributorSection = new ContributorSection(page);
      const descriptionSection = new DescriptionSection(page);
      const altIdSection = new AlternateIdentifierSection(page);
      const altUrlSection = new AlternateUrlSection(page);
      const relatedObjectSection = new RelatedObjectSection(page);
      const relatedRaidSection = new RelatedRaidSection(page);
      const spatialCoverageSection = new SpatialCoverageSection(page);

      // --- Navigate to the create page ---
      await formPage.goto("/raids/new");

      // =========================================================
      // REQUIRED FIELDS
      // =========================================================

      // Title
      await titleSection.fillText(0, TEST_TITLE);

      // Date
      await dateSection.fillStartDate(START_DATE);

      // Access — Embargoed so the statement field is rendered
      await accessSection.selectAccessType(EMBARGOED_LABEL);
      await accessSection.fillStatementText(ACCESS_STATEMENT);
      await accessSection.fillEmbargoExpiry(EMBARGO_EXPIRY);

      // Contributors — primary (leader + contact) and a second contributor
      await contributorSection.addItem();
      await contributorSection.fillOrcidId(0, ORCID_PRIMARY);
      // Note: CheckboxField does not set an HTML id, so checkLeader/checkContact
      // cannot be addressed by #contributor\.N\.leader. The data generator
      // pre-fills leader=true for the first contributor, so this is not needed.

      await contributorSection.addItem();
      await contributorSection.fillOrcidId(1, ORCID_SECONDARY);

      // =========================================================
      // OPTIONAL: Description
      // =========================================================
      await descriptionSection.addItem();
      await descriptionSection.fillText(0, DESCRIPTION_TEXT);
      await descriptionSection.selectType(0, DESCRIPTION_TYPE_LABEL);
      await descriptionSection.selectLanguage(0, "eng", /eng.*English/i);

      // =========================================================
      // OPTIONAL: Alternate Identifier
      // =========================================================
      await altIdSection.addItem();
      await altIdSection.fillId(0, ALT_ID_VALUE);
      await altIdSection.fillType(0, ALT_ID_TYPE);

      // =========================================================
      // OPTIONAL: Alternate URL
      // =========================================================
      await altUrlSection.addItem();
      await altUrlSection.fillUrl(0, ALT_URL);

      // =========================================================
      // OPTIONAL: Related Object (DOI + type + category)
      // =========================================================
      await relatedObjectSection.addItem();
      await relatedObjectSection.fillId(0, RELATED_OBJECT_DOI);
      await relatedObjectSection.selectType(0, RELATED_OBJECT_TYPE_LABEL);

      // The data generator pre-populates one category at index 0 when
      // "Add Related Object" is clicked. Select the category type in that
      // pre-existing slot — do NOT click "Add Category" first or a duplicate
      // will be created.
      await relatedObjectSection.selectCategory(0, 0, RELATED_OBJECT_CATEGORY_LABEL);

      // =========================================================
      // OPTIONAL: Related RAiD (handle + type)
      // =========================================================
      await relatedRaidSection.addItem();
      await relatedRaidSection.fillId(0, RELATED_RAID_HANDLE);
      await relatedRaidSection.selectType(0, RELATED_RAID_TYPE_LABEL);

      // =========================================================
      // OPTIONAL: Spatial Coverage (URI + place name)
      // =========================================================
      await spatialCoverageSection.addItem();
      await spatialCoverageSection.fillId(0, SPATIAL_COVERAGE_URI);

      // The "Places" sub-card requires adding a place entry before filling it.
      await spatialCoverageSection.addPlace(0);
      await spatialCoverageSection.fillPlaceName(0, 0, SPATIAL_COVERAGE_PLACE);

      // =========================================================
      // Save and verify
      // =========================================================
      await formPage.save();
      await formPage.waitForSuccessfulSave();

      // Capture the handle for the second test
      const url = page.url();
      [createdPrefix, createdSuffix] = extractPrefixSuffixFromUrl(url);

      // Success snackbar should appear
      await waitForSnackbar(page, "Raid created successfully");

      // View page should show the title we entered
      await expect(
        page.locator("p").filter({ hasText: TEST_TITLE }).first()
      ).toBeVisible({ timeout: 15000 });

      // Confirm we are on the view URL (not the create URL)
      await expect(page).toHaveURL(/\/raids\/[^/]+\/[^/]+$/);
      await expect(page).not.toHaveURL(/\/raids\/new/);
    }
  );

  test(
    "view page displays the optional metadata that was saved",
    { tag: "@local" },
    async ({ page }) => {
      // Navigate directly to the view page for the created RAiD.
      // The view page does not use a semantic <main> element; wait for the
      // breadcrumb which is always rendered once the data is loaded.
      // The breadcrumb is a <div aria-label="breadcrumb">, not a <nav>, so
      // getByRole("navigation") won't match — use getByLabel instead.
      await page.goto(`/raids/${createdPrefix}/${createdSuffix}`);
      await expect(
        page.getByLabel("breadcrumb")
      ).toBeVisible({ timeout: 15000 });

      // Title
      await expect(
        page.locator("p").filter({ hasText: TEST_TITLE }).first()
      ).toBeVisible({ timeout: 15000 });

      // All assertions below scope to <p> elements to avoid strict-mode conflicts
      // with the raw JSON <pre> block that contains the same text verbatim.

      // Description text
      await expect(
        page.locator("p").filter({ hasText: DESCRIPTION_TEXT }).first()
      ).toBeVisible({ timeout: 10000 });

      // Alternate identifier
      await expect(
        page.locator("p").filter({ hasText: ALT_ID_VALUE }).first()
      ).toBeVisible({ timeout: 10000 });

      // Alternate URL
      await expect(
        page.locator("p").filter({ hasText: ALT_URL }).first()
      ).toBeVisible({ timeout: 10000 });

      // Related object DOI
      await expect(
        page.locator("p").filter({ hasText: RELATED_OBJECT_DOI }).first()
      ).toBeVisible({ timeout: 10000 });

      // Spatial coverage URI
      await expect(
        page.locator("p").filter({ hasText: SPATIAL_COVERAGE_URI }).first()
      ).toBeVisible({ timeout: 10000 });
    }
  );
});
