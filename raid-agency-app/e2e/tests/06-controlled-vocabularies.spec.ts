// RAID-541: E2E tests for controlled vocabularies
// Subtask of RAID-535
//
// Tests that form dropdowns (MUI Selects) contain the expected options from
// controlled vocabularies. Each test navigates to /raids/new, opens a
// dropdown, collects visible options, and asserts the expected labels are
// present. Tests are independent — they only read the form, never submit it.
//
// Label sources:
//   - Title types, Description types, Access types, Contributor positions,
//     Organisation roles, Related object types/categories, Related RAiD types:
//       src/mapping/data/general-mapping.json
//   - Contributor roles:
//       src/references/contributor_role.json — labels are the raw URI strings
//       (the form maps { value: el.uri, label: el.uri })
//
// Note: Contributor position and role dropdowns only appear after
// "Add Contributor" is clicked, since the data generator pre-populates one
// position and one role entry when the item is first added.
// Similarly, Related object category dropdowns only appear after
// "Add Related Object" is clicked.
// Related RAiD type dropdowns only appear after "Add Related RAiD" is clicked.
//
// Local environment notes:
//   - No network calls are made — tests are read-only form inspections

import { test } from "@playwright/test";
import { RaidFormPage } from "../page-objects/RaidFormPage";
import { assertSelectOptions } from "../utils/mui-helpers";
import { CONTRIBUTOR_ROLE } from "../test-data/vocabulary";

// ---------------------------------------------------------------------------
// Title types
// ---------------------------------------------------------------------------

test.describe("Controlled vocabulary: Title types", () => {
  test(
    "title type dropdown contains all expected options",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      await formPage.goto("/raids/new");

      // Title section pre-fills index 0; the type select is already rendered
      await assertSelectOptions(
        page,
        "title\\.0\\.type\\.id",
        ["Primary", "Alternative", "Acronym", "Short"]
      );
    }
  );
});

// ---------------------------------------------------------------------------
// Description types
// ---------------------------------------------------------------------------

test.describe("Controlled vocabulary: Description types", () => {
  test(
    "description type dropdown contains all expected options",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      await formPage.goto("/raids/new");

      // Add a description item to make the type select appear
      await page.locator("#description").getByRole("button", { name: "Add Description" }).click();

      await assertSelectOptions(
        page,
        "description\\.0\\.type\\.id",
        [
          "Primary",
          "Alternative",
          "Brief",
          "Methods",
          "Objectives",
          "Other",
          "Significance statement",
          "Acknowledgements",
        ]
      );
    }
  );
});

// ---------------------------------------------------------------------------
// Access types
// ---------------------------------------------------------------------------

test.describe("Controlled vocabulary: Access types", () => {
  test(
    "access type dropdown contains Open Access and Embargoed Access",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      await formPage.goto("/raids/new");

      await assertSelectOptions(
        page,
        "access\\.type\\.id",
        ["Open Access", "Embargoed Access"]
      );
    }
  );
});

// ---------------------------------------------------------------------------
// Contributor positions
// ---------------------------------------------------------------------------

test.describe("Controlled vocabulary: Contributor positions", () => {
  test(
    "contributor position dropdown contains all expected options",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      await formPage.goto("/raids/new");

      // The contributor section pre-fills a position entry when a contributor
      // is added via "Add Contributor"
      await page.locator("#contributor").getByRole("button", { name: "Add Contributor" }).click();

      await assertSelectOptions(
        page,
        "contributor\\.0\\.position\\.0\\.id",
        [
          "Principal or Chief Investigator",
          "Co-investigator or Collaborator",
          "Partner Investigator",
          "Consultant",
          "Other Participant",
        ]
      );
    }
  );
});

// ---------------------------------------------------------------------------
// Contributor roles
// ---------------------------------------------------------------------------

test.describe("Controlled vocabulary: Contributor roles", () => {
  test(
    "contributor role dropdown contains all 14 CRediT role URIs",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      await formPage.goto("/raids/new");

      // Add a contributor — the data generator pre-populates a role entry
      await page.locator("#contributor").getByRole("button", { name: "Add Contributor" }).click();

      // Contributor roles use the URI as the display label
      await assertSelectOptions(
        page,
        "contributor\\.0\\.role\\.0\\.id",
        Object.values(CONTRIBUTOR_ROLE)
      );
    }
  );
});

// ---------------------------------------------------------------------------
// Organisation roles
// ---------------------------------------------------------------------------

test.describe("Controlled vocabulary: Organisation roles", () => {
  test(
    "organisation role dropdown contains all expected options",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      await formPage.goto("/raids/new");

      // Add an organisation item to make the role select appear
      await page.locator("#organisation").getByRole("button", { name: "Add Organisation" }).click();

      await assertSelectOptions(
        page,
        "organisation\\.0\\.role\\.0\\.id",
        [
          "Lead Research Organisation",
          "Other Research Organisation",
          "Partner Organisation",
          "Contractor",
          "Funder",
          "Facility",
          "Other Organisation",
        ]
      );
    }
  );
});

// ---------------------------------------------------------------------------
// Related object types
// ---------------------------------------------------------------------------

test.describe("Controlled vocabulary: Related object types", () => {
  test(
    "related object type dropdown contains key expected types",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      await formPage.goto("/raids/new");

      // Add a related object item to make the type select appear
      await page.locator("#relatedObject").getByRole("button", { name: "Add Related Object" }).click();

      await assertSelectOptions(
        page,
        "relatedObject\\.0\\.type\\.id",
        [
          "Journal Article",
          "Dataset",
          "Software",
          "Report",
          "Conference Paper",
        ]
      );
    }
  );
});

// ---------------------------------------------------------------------------
// Related object categories
// ---------------------------------------------------------------------------

test.describe("Controlled vocabulary: Related object categories", () => {
  test(
    "related object category dropdown contains Input, Output, and Internal process document or artefact",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      await formPage.goto("/raids/new");

      // Add a related object item — the data generator pre-populates one
      // category entry at index 0
      await page.locator("#relatedObject").getByRole("button", { name: "Add Related Object" }).click();

      await assertSelectOptions(
        page,
        "relatedObject\\.0\\.category\\.0\\.id",
        [
          "Input",
          "Output",
          "Internal process document or artefact",
        ]
      );
    }
  );
});

// ---------------------------------------------------------------------------
// Related RAiD types
// ---------------------------------------------------------------------------

test.describe("Controlled vocabulary: Related RAiD types", () => {
  test(
    "related RAiD type dropdown contains all expected options",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      await formPage.goto("/raids/new");

      // Add a related RAiD item to make the type select appear
      await page.locator("#relatedRaid").getByRole("button", { name: "Add Related RAiD" }).click();

      await assertSelectOptions(
        page,
        "relatedRaid\\.0\\.type\\.id",
        [
          "Continues",
          "HasPart",
          "IsContinuedBy",
          "IsDerivedFrom",
          "IsObsoletedBy",
          "IsPartOf",
          "IsSourceOf",
          "Obsoletes",
        ]
      );
    }
  );
});
