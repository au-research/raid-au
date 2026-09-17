// RAID-801: E2E tests for Handle/RRID/ARK recognition in the Related Object
// bulk-upload spreadsheet, plus a regression guard for an unrecognised
// identifier.
//
// Handle (RAID-786) and RRID (RAID-787) have real backend validators, so
// this only exercises frontend classification via the preview table (no
// bulk-upload row reaches "Confirm upload" here) — matching the scope of
// this Story, which is the spreadsheet's row classification, not the
// backend validators themselves (already covered elsewhere). ARK's backend
// validator (RAID-793) hasn't merged yet, so — same precedent as RAID-800's
// own related-object identifier spec — the ARK row is only asserted as
// correctly classified in the preview, not submitted end to end.

import path from "path";
import { fileURLToPath } from "url";
import { test, expect } from "@playwright/test";
import { RaidFormPage } from "../page-objects/RaidFormPage";
import { TitleSection } from "../page-objects/sections/TitleSection";
import { DateSection } from "../page-objects/sections/DateSection";
import { AccessSection } from "../page-objects/sections/AccessSection";
import { RelatedObjectSection } from "../page-objects/sections/RelatedObjectSection";
import { validEmbargoExpiry } from "../utils/date-helpers";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const START_DATE = "2024-03-01";
const EMBARGOED_LABEL = "Embargoed Access";
const ACCESS_STATEMENT = "Embargoed for bulk-upload identifier inference e2e testing";
const EMBARGO_EXPIRY = validEmbargoExpiry();

const FIXTURE_PATH = path.join(
  __dirname,
  "..",
  "fixtures",
  "bulk-upload-handle-rrid-ark.csv"
);

const HANDLE_URL = "https://hdl.handle.net/20.500.12345/abc123";
const RRID_URL = "https://scicrunch.org/resolver/RRID:AB_2298772";
const ARK_URL = "https://example-repository.edu/ark:/13030/kt6f59n8z3";
const BAD_URL = "https://example.com/some-unrelated-path";

async function setUpFormAndUploadFixture(page: import("@playwright/test").Page) {
  const formPage = new RaidFormPage(page);
  const titleSection = new TitleSection(page);
  const dateSection = new DateSection(page);
  const accessSection = new AccessSection(page);
  const relatedObjectSection = new RelatedObjectSection(page);

  await formPage.goto("/raids/new");
  await titleSection.fillText(0, `E2E Bulk Upload Identifier Inference Test ${Date.now()}`);
  await dateSection.fillStartDate(START_DATE);
  await accessSection.selectAccessType(EMBARGOED_LABEL);
  await accessSection.fillStatementText(ACCESS_STATEMENT);
  await accessSection.fillEmbargoExpiry(EMBARGO_EXPIRY);

  await relatedObjectSection.openBulkUpload();
  await relatedObjectSection.uploadBulkFile(FIXTURE_PATH);

  return { relatedObjectSection };
}

test.describe("Bulk upload related-object identifier recognition", { tag: "@local" }, () => {
  test("Handle, RRID, and ARK rows are classified and accepted, an unrecognised identifier still fails", async ({
    page,
  }) => {
    const { relatedObjectSection } = await setUpFormAndUploadFixture(page);

    const handleInput = relatedObjectSection.bulkPreviewRow(HANDLE_URL).locator("input").first();
    const rridInput = relatedObjectSection.bulkPreviewRow(RRID_URL).locator("input").first();
    const arkInput = relatedObjectSection.bulkPreviewRow(ARK_URL).locator("input").first();
    const badInput = relatedObjectSection.bulkPreviewRow(BAD_URL).locator("input").first();

    await expect(handleInput).toHaveAttribute("aria-invalid", "false");
    await expect(rridInput).toHaveAttribute("aria-invalid", "false");
    await expect(arkInput).toHaveAttribute("aria-invalid", "false");

    // Regression guard: an identifier matching no recognised scheme still
    // fails the same way it did before Handle/RRID/ARK were recognised.
    await expect(badInput).toHaveAttribute("aria-invalid", "true");

    await expect(page.getByText("1 row has errors", { exact: true })).toBeVisible();
  });
});
