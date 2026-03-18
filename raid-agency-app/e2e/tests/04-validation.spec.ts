// RAID-539: E2E validation tests for mandatory fields and formats
// Subtask of RAID-535
//
// Tests that the RAiD create form enforces client-side validation via
// React Hook Form + Zod. Each test is independent: it navigates to
// /raids/new, manipulates specific fields, clicks Save, and asserts
// that the expected validation error appears without the form submitting.
//
// Validation enforced client-side (tested here):
//   - Title text is required (z.string().min(1))
//   - Date fields must match YYYY, YYYY-MM, or YYYY-MM-DD format
//   - ORCID ID must match https://orcid.org/XXXX-XXXX-XXXX-XXXX pattern
//   - End date must be >= start date (refine on both date and title schemas)
//   - Embargo expiry date must match YYYY-MM-DD format
//
// Scenarios NOT enforced client-side (skipped with TODO):
//   - Empty contributor array: the Zod schema uses .min(1) on the array
//     but the form initialises with zero contributors and appears to allow
//     Save without any contributor rows (server will reject). The form does
//     not add a contributor row on its own, so clicking Save with no
//     contributors causes a network POST, not a client-side validation halt.
//     TODO: investigate whether contributorValidationSchema.min(1) surfaces
//     a visible error element in the UI when the contributor array is empty.
//   - Access statement required when Embargoed: the Zod schema marks
//     statement.text as optional (z.string().optional()) even in the
//     embargoed branch, so the client will not reject an empty statement.
//
// Local environment notes:
//   - The ORCID regex in the Zod schema only accepts production orcid.org
//     (not sandbox.orcid.org) — sandbox is accepted by the local API server
//     but the Zod pattern is stricter.
//   - combinedPattern allows an empty string (the group is wrapped in `?`),
//     so an empty startDate field will NOT trigger a date-format error;
//     only an actively malformed value (e.g. "not-a-date") will.

import { test, expect } from "@playwright/test";
import { RaidFormPage } from "../page-objects/RaidFormPage";
import { TitleSection } from "../page-objects/sections/TitleSection";
import { DateSection } from "../page-objects/sections/DateSection";
import { AccessSection } from "../page-objects/sections/AccessSection";
import { ContributorSection } from "../page-objects/sections/ContributorSection";

// Shared constants
const VALID_START_DATE = "2024-01-15";
const VALID_TITLE = "E2E Validation Test";
// Uses production orcid.org format to pass the client-side Zod regex.
// The API may reject it, but validation tests never save successfully —
// we only need the ORCID field to pass client-side validation.
const VALID_ORCID = "https://orcid.org/0000-0000-0000-0000";
const EMBARGOED_LABEL = "Embargoed Access";
const VALID_EMBARGO_EXPIRY = "2030-01-01";
const VALID_ACCESS_STATEMENT = "Embargoed for e2e validation testing";

test.describe("Validation: title text is required", () => {
  test(
    "shows error when title text is empty and Save is clicked",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      const titleSection = new TitleSection(page);

      await formPage.goto("/raids/new");

      // Clear the title text field (the form pre-fills nothing in the text field,
      // but we explicitly clear to be sure)
      await titleSection.fillText(0, "");

      await formPage.save();

      // The Zod message from z.string().min(1) is "String must contain at least 1 character(s)".
      // Match a substring to be resilient to Zod version changes.
      await expect(
        page.locator("text=at least 1 character").first()
      ).toBeVisible({ timeout: 5000 });
    }
  );
});

test.describe("Validation: date format", () => {
  test(
    "shows error when start date has an invalid format",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      const titleSection = new TitleSection(page);
      const dateSection = new DateSection(page);

      await formPage.goto("/raids/new");

      await titleSection.fillText(0, VALID_TITLE);

      // Enter a value that does not match YYYY, YYYY-MM, or YYYY-MM-DD
      await dateSection.fillStartDate("not-a-date");

      await formPage.save();

      // The error message from the Zod schema is "YYYY or YYYY-MM or YYYY-MM-DD"
      await expect(
        page.locator("text=YYYY or YYYY-MM or YYYY-MM-DD").first()
      ).toBeVisible({ timeout: 5000 });
    }
  );

  test(
    "shows error when end date is before start date",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      const titleSection = new TitleSection(page);
      const dateSection = new DateSection(page);
      const accessSection = new AccessSection(page);
      const contributorSection = new ContributorSection(page);

      await formPage.goto("/raids/new");

      await titleSection.fillText(0, VALID_TITLE);
      await dateSection.fillStartDate("2024-06-01");
      // Set end date strictly before start date
      await dateSection.fillEndDate("2024-01-01");

      // Fill minimum required fields so the only validation error is the date order
      await accessSection.selectAccessType(EMBARGOED_LABEL);
      await accessSection.fillStatementText(VALID_ACCESS_STATEMENT);
      await accessSection.fillEmbargoExpiry(VALID_EMBARGO_EXPIRY);
      await contributorSection.addItem();
      await contributorSection.fillOrcidId(0, VALID_ORCID);

      await formPage.save();

      // The refine message is "Start date must be before or equal to end date"
      await expect(
        page.locator("text=Start date must be before or equal to end date").first()
      ).toBeVisible({ timeout: 5000 });
    }
  );
});

test.describe("Validation: ORCID format", () => {
  test(
    "shows error when contributor ORCID ID has an invalid format",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      const titleSection = new TitleSection(page);
      const dateSection = new DateSection(page);
      const accessSection = new AccessSection(page);
      const contributorSection = new ContributorSection(page);

      await formPage.goto("/raids/new");

      await titleSection.fillText(0, VALID_TITLE);
      await dateSection.fillStartDate(VALID_START_DATE);
      await accessSection.selectAccessType(EMBARGOED_LABEL);
      await accessSection.fillStatementText(VALID_ACCESS_STATEMENT);
      await accessSection.fillEmbargoExpiry(VALID_EMBARGO_EXPIRY);

      await contributorSection.addItem();
      // Enter a clearly invalid ORCID — not a URL, wrong format
      await contributorSection.fillOrcidId(0, "not-an-orcid");

      await formPage.save();

      // The error message from the Zod schema:
      // "Invalid ORCID ID, must be full url, e.g. https://orcid.org/0000-0000-0000-0000"
      await expect(
        page.locator("text=Invalid ORCID ID").first()
      ).toBeVisible({ timeout: 5000 });
    }
  );
});

test.describe("Validation: embargo expiry date format", () => {
  test(
    "shows error when embargo expiry date has an invalid format",
    { tag: "@local" },
    async ({ page }) => {
      const formPage = new RaidFormPage(page);
      const titleSection = new TitleSection(page);
      const dateSection = new DateSection(page);
      const accessSection = new AccessSection(page);
      const contributorSection = new ContributorSection(page);

      await formPage.goto("/raids/new");

      await titleSection.fillText(0, VALID_TITLE);
      await dateSection.fillStartDate(VALID_START_DATE);
      await accessSection.selectAccessType(EMBARGOED_LABEL);
      await accessSection.fillStatementText(VALID_ACCESS_STATEMENT);
      // Enter a value that does not match the strict YYYY-MM-DD pattern
      // The embargo expiry uses yearMonthDayPatternSchema, which requires YYYY-MM-DD
      // (unlike the start/end date fields which also accept YYYY and YYYY-MM)
      await accessSection.fillEmbargoExpiry("2030");

      await contributorSection.addItem();
      await contributorSection.fillOrcidId(0, VALID_ORCID);

      await formPage.save();

      // The error message from yearMonthDayPatternSchema is "YYYY-MM-DD"
      await expect(
        page.locator("text=YYYY-MM-DD").first()
      ).toBeVisible({ timeout: 5000 });
    }
  );
});
