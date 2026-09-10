// RAID-861 / bug/isni-helpertext: E2E tests for auto-detect schemaUri
// recognition on the Contributor identifier field (ISNI), plus a regression
// guard confirming the existing ORCID lookup widget behaviour is unaffected.
//
// The backend has a real ISNI validator (ISO 7064 MOD 11-2 check-digit, see
// ContributorValidator/IsniValidator) but resolves ISNI IDs against a live
// external resolver that isn't stubbed in local/dev (unlike ORCID's
// sandbox), so a save attempt here surfaces a "Resolver unavailable"
// dialog. The ISNI test therefore only asserts the outgoing create-RAiD
// request payload (schemaUri + id) and the plain-field UI, matching the
// same-situation precedent for ARK (RAID-793) in
// 07-related-object-identifier-inference on the RAID-800 branch. The ORCID
// test asserts a real successful save, since that path is unchanged and
// fully supported end to end. 0000000121032683 is a real, valid ISNI
// (correct check digit), not a placeholder.

import { test, expect } from "@playwright/test";
import { RaidFormPage } from "../page-objects/RaidFormPage";
import { TitleSection } from "../page-objects/sections/TitleSection";
import { DateSection } from "../page-objects/sections/DateSection";
import { AccessSection } from "../page-objects/sections/AccessSection";
import { ContributorSection } from "../page-objects/sections/ContributorSection";
import { validEmbargoExpiry } from "../utils/date-helpers";
import { extractPrefixSuffixFromUrl } from "../utils/wait-helpers";

const START_DATE = "2024-03-01";
const EMBARGOED_LABEL = "Embargoed Access";
const ACCESS_STATEMENT = "Embargoed for contributor-identifier inference e2e testing";
const EMBARGO_EXPIRY = validEmbargoExpiry();
const ISNI_URL = "https://isni.org/0000000121032683";
// This ISNI has a matching expectation in the local mockserver
// (docker-compose/mockserver/expectations.json), so unlike ISNI_URL above it
// resolves and saves successfully end to end in local/dev - needed for the
// view-page test below, which must get past a real save to render.
const MOCKED_ISNI_URL = "https://isni.org/0000000078519858";
const ORCID_URL = "https://sandbox.orcid.org/0009-0002-5128-5184";

interface ContributorPayload {
  contributor?: Array<{ id?: string; schemaUri?: string }>;
}

// Fills the minimal set of required fields so the form can be submitted,
// then adds one empty Contributor row ready to receive a pasted id.
async function setUpFormWithContributorRow(page: import("@playwright/test").Page) {
  const formPage = new RaidFormPage(page);
  const titleSection = new TitleSection(page);
  const dateSection = new DateSection(page);
  const accessSection = new AccessSection(page);
  const contributorSection = new ContributorSection(page);

  await formPage.goto("/raids/new");
  await titleSection.fillText(0, `E2E Contributor Identifier Inference Test ${Date.now()}`);
  await dateSection.fillStartDate(START_DATE);
  await accessSection.selectAccessType(EMBARGOED_LABEL);
  await accessSection.fillStatementText(ACCESS_STATEMENT);
  await accessSection.fillEmbargoExpiry(EMBARGO_EXPIRY);

  await contributorSection.addItem();

  return { formPage, contributorSection };
}

test.describe("Contributor identifier auto-detect", { tag: "@local" }, () => {
  test("pasting an ISNI URL infers the ISNI schemaUri and shows a plain identifier field", async ({
    page,
  }) => {
    const { formPage, contributorSection } = await setUpFormWithContributorRow(page);

    // bug/isni-helpertext: the helper-text row must reserve enough height
    // for the longer ISNI copy up front, so switching from the ORCID
    // helper text to the ISNI one doesn't shift the rest of the form down.
    const helperTextRow = page
      .locator("#contributor")
      .getByText(/Use the ORCID sandbox|Enter a valid ORCID iD|Enter a valid ISNI URL/)
      .locator("..");
    const heightBefore = (await helperTextRow.boundingBox())?.height;

    await contributorSection.fillOrcidId(0, ISNI_URL);

    // Plain-field UI for ISNI: the name-line row stays mounted (avoids a
    // layout jump) but swaps to an ISNI-specific message, and there's no
    // lookup/search button.
    await expect(page.getByText(/^Name:/)).not.toBeVisible();
    await expect(page.getByText("The entered ID is an ISNI")).toBeVisible();
    await expect(page.locator('[aria-label="directions"]')).toHaveCount(0);

    // bug/isni-helpertext: the helper text and tooltip must switch to
    // ISNI-specific copy too, rather than continuing to describe ORCID's
    // name-lookup behaviour once an ISNI has been entered.
    // The Contributor card also has its own section-header info tooltip
    // sharing the same static id, so scope to the last one - the ORCID/ISNI
    // identifier field's own tooltip, rendered after it.
    const contributorTooltipButton = page.locator("#contributor #tooltip-button").last();
    await expect(page.getByText(/Enter a valid ISNI URL/)).toBeVisible();
    expect((await helperTextRow.boundingBox())?.height).toBe(heightBefore);
    await expect(page.getByText(/Enter a valid ORCID iD/)).not.toBeVisible();
    await contributorTooltipButton.click();
    await expect(page.getByText("ISNI Info")).toBeVisible();
    await expect(page.getByText(/Credit Name/)).not.toBeVisible();
    await contributorTooltipButton.click(); // close it before saving

    const [request] = await Promise.all([
      page.waitForRequest(
        (req) => req.method() === "POST" && /\/raid\/?$/.test(new URL(req.url()).pathname)
      ),
      formPage.save(),
    ]);

    const body = request.postDataJSON() as ContributorPayload;
    const contributor = body.contributor?.[0];
    expect(contributor?.schemaUri).toBe("https://isni.org/");
    expect(contributor?.id).toBe(ISNI_URL);
  });

  test("pasting an ORCID iD still resolves a name and saves successfully (regression guard)", async ({
    page,
  }) => {
    const { formPage, contributorSection } = await setUpFormWithContributorRow(page);

    await contributorSection.searchAndSelectOrcid(0, ORCID_URL);

    await expect(page.getByText(/^Name:/)).toBeVisible();
    await expect(page.locator('[aria-label="directions"]')).toHaveCount(1);

    // bug/isni-helpertext regression guard: ORCID's own helper text/tooltip
    // must stay exactly as before - no ISNI copy has leaked in. (The exact
    // ORCID helper text is environment-configurable via app-config.json's
    // orcid.helpText, so we only assert the ISNI-specific strings are absent
    // and the ORCID tooltip title is unchanged.)
    // The Contributor card also has its own section-header info tooltip
    // sharing the same static id, so scope to the last one - the ORCID/ISNI
    // identifier field's own tooltip, rendered after it.
    const contributorTooltipButton = page.locator("#contributor #tooltip-button").last();
    await expect(page.getByText(/Enter a valid ISNI URL/)).not.toBeVisible();
    await contributorTooltipButton.click();
    await expect(page.getByText("ORCID Lookup Info")).toBeVisible();
    await expect(page.getByText("ISNI Info")).not.toBeVisible();
    await contributorTooltipButton.click();

    await formPage.save();
    await formPage.waitForSuccessfulSave();

    // Regression guard: the view page must keep showing ORCID's own
    // authenticated/unauthenticated icon and label for an ORCID contributor.
    await expect(page.getByAltText(/authenticated/i)).toBeVisible();
    await expect(page.getByText("ORCID", { exact: true }).first()).toBeVisible();
  });

  test("RAiD view page hides the ORCID icon and authenticated/unauthenticated text for an ISNI contributor", async ({
    page,
  }) => {
    const { formPage, contributorSection } = await setUpFormWithContributorRow(page);

    await contributorSection.fillOrcidId(0, MOCKED_ISNI_URL);
    await formPage.save();
    await formPage.waitForSuccessfulSave();

    // Bug fix: the ORCID authenticated/unauthenticated icon and status text
    // don't apply to ISNI (no OAuth flow), and the "ORCID" label is wrong
    // for an ISNI value - the view page must not show any of them.
    await expect(page.getByAltText(/authenticated/i)).not.toBeVisible();
    await expect(page.getByText(/unauthenticated/i)).not.toBeVisible();
    await expect(page.getByText("ISNI", { exact: true })).toBeVisible();
    await expect(page.getByText("ORCID", { exact: true })).not.toBeVisible();
    await expect(page.getByText(MOCKED_ISNI_URL).first()).toBeVisible();
  });

  test("RAiD edit page keeps the ISNI identifier editable instead of showing an ORCID-style status", async ({
    page,
  }) => {
    const { formPage, contributorSection } = await setUpFormWithContributorRow(page);

    await contributorSection.fillOrcidId(0, MOCKED_ISNI_URL);
    await formPage.save();
    await formPage.waitForSuccessfulSave();

    const [prefix, suffix] = extractPrefixSuffixFromUrl(page.url());
    await formPage.goto(`/raids/${prefix}/${suffix}/edit`);

    // Bug fix: once a saved contributor has a "status" field, ORCID's
    // identifier field locks and shows a read-only "Contributor Status"
    // (e.g. AWAITING_AUTHENTICATION) instead - that status concept doesn't
    // apply to ISNI (no OAuth flow), so the field must stay editable and
    // pre-filled with the existing ISNI, with no status text shown.
    await expect(page.getByText(/AWAITING_AUTHENTICATION|AUTHENTICATED|UNAUTHENTICATED/)).not.toBeVisible();
    await expect(page.locator('#contributor input[aria-label="search orcid"]')).toHaveValue(MOCKED_ISNI_URL);
  });

  test("correcting an invalid id to a valid ISNI lets the next save succeed without a stale error", async ({
    page,
  }) => {
    const { formPage, contributorSection } = await setUpFormWithContributorRow(page);

    // Trigger the "Invalid ORCID ID" validation error dialog first (same
    // setup as 04-validation.spec.ts's ORCID-format test).
    await contributorSection.fillOrcidId(0, "not-an-orcid");
    await formPage.save();
    await expect(page.locator("text=Invalid ORCID ID").first()).toBeVisible({
      timeout: 5000,
    });
    await page.getByRole("button", { name: "Close", exact: true }).last().click();

    // Bug fix: the id field's setValue call omitted shouldValidate, so RHF's
    // internal error state for this field never actually cleared once a
    // corrected value was entered - re-saving kept reopening the exact same
    // stale "Invalid ORCID ID" dialog even though the id was now a valid,
    // recognised ISNI.
    await contributorSection.fillOrcidId(0, ISNI_URL);
    await formPage.save();
    await expect(page.locator("text=Invalid ORCID ID").first()).not.toBeVisible({
      timeout: 5000,
    });
  });
});
