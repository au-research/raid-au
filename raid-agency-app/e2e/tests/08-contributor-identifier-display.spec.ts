// RAID-920: E2E tests for showing an existing Contributor's identifier and
// live-resolved name as a read-only display on both the RAiD View page and
// Edit page, for both ORCID and ISNI.
//
// ORCID resolves via its own public API, called directly from the browser.
// ISNI has no equivalent public, CORS-enabled API (confirmed live during
// development: isni.oclc.org's Access-Control-Allow-Origin header is a
// fixed value regardless of the calling origin, so a direct browser call
// would always be rejected) - ISNI resolution instead goes through a small
// new backend endpoint (GET /isni/{isni}/name) wrapping the existing,
// already-tested IsniClient. The Edit-page read-only/pencil-toggle
// behaviour for ISNI is covered in 07-contributor-identifier-inference.spec.ts
// (RAID-883's test), not duplicated here.

import { test, expect, type Page } from "@playwright/test";
import { RaidFormPage } from "../page-objects/RaidFormPage";
import { RaidViewPage } from "../page-objects/RaidViewPage";
import { TitleSection } from "../page-objects/sections/TitleSection";
import { DateSection } from "../page-objects/sections/DateSection";
import { AccessSection } from "../page-objects/sections/AccessSection";
import { ContributorSection } from "../page-objects/sections/ContributorSection";
import { validEmbargoExpiry } from "../utils/date-helpers";
import { extractPrefixSuffixFromUrl } from "../utils/wait-helpers";

const START_DATE = "2024-03-01";
const EMBARGOED_LABEL = "Embargoed Access";
const ACCESS_STATEMENT = "Embargoed for contributor-identifier display e2e testing";
const EMBARGO_EXPIRY = validEmbargoExpiry();
// A real, resolvable sandbox ORCID iD (same fixture already relied on by
// 07-contributor-identifier-inference.spec.ts's ORCID regression guard).
const ORCID_URL = "https://sandbox.orcid.org/0009-0002-5128-5184";
// This ISNI has a matching expectation in the local mockserver
// (docker-compose/mockserver/expectations.json), so it resolves and saves
// successfully end to end in local/dev.
const MOCKED_ISNI_URL = "https://isni.org/0000000078519858";

async function setUpFormWithContributorRow(page: Page) {
  const formPage = new RaidFormPage(page);
  const titleSection = new TitleSection(page);
  const dateSection = new DateSection(page);
  const accessSection = new AccessSection(page);
  const contributorSection = new ContributorSection(page);

  await formPage.goto("/raids/new");
  await titleSection.fillText(0, `E2E Contributor Identifier Display Test ${Date.now()}`);
  await dateSection.fillStartDate(START_DATE);
  await accessSection.selectAccessType(EMBARGOED_LABEL);
  await accessSection.fillStatementText(ACCESS_STATEMENT);
  await accessSection.fillEmbargoExpiry(EMBARGO_EXPIRY);

  await contributorSection.addItem();

  return { formPage, contributorSection };
}

// Saves a new RAiD with the given identifier already filled in, returning
// [prefix, suffix] - the browser ends up on the resulting view page.
async function saveWithIdentifier(
  page: Page,
  contributorSection: ContributorSection,
  formPage: RaidFormPage,
  identifierUrl: string
) {
  await contributorSection.fillOrcidId(0, identifierUrl);
  await formPage.save();
  await formPage.waitForSuccessfulSave();
  return extractPrefixSuffixFromUrl(page.url());
}

// The backend itself verifies an ORCID exists in the registry before a save
// is accepted ("contributor[0].id: This id does not exist") - so a
// never-registered-but-checksum-valid ORCID can never reach a saved RAiD at
// all, and isn't a usable fixture for the "lookup fails" scenario. Instead,
// save a real resolvable ORCID normally, then intercept the browser's own
// client-side call to the public ORCID API (made independently by
// ContributorItemView/ContributorDetailsForm to resolve a display name) and
// force it to fail - this exercises exactly the failure path those
// components handle, without depending on ORCID registry contents.
async function failOrcidPublicApiLookups(page: Page) {
  await page.route("**/pub.sandbox.orcid.org/v3.0/**", (route) =>
    route.fulfill({ status: 404, contentType: "application/json", body: "{}" })
  );
}

// Mirrors failOrcidPublicApiLookups above, but for our own backend's ISNI
// name endpoint rather than a third-party API - a never-registered ISNI
// can't be used for this instead, because the backend's own save-time
// existence check (a separate call, also against the ISNI resolver) would
// reject the save outright before a RAiD even exists to view/edit.
async function failIsniNameLookups(page: Page) {
  await page.route("**/isni/**/name", (route) =>
    route.fulfill({ status: 404, contentType: "application/json", body: "{}" })
  );
}

test.describe("Contributor identifier + name display", { tag: "@local" }, () => {
  test("View page shows an existing ORCID contributor's identifier and resolved name", async ({
    page,
  }) => {
    const { formPage, contributorSection } = await setUpFormWithContributorRow(page);
    await saveWithIdentifier(page, contributorSection, formPage, ORCID_URL);

    const viewPage = new RaidViewPage(page);
    await expect(page.getByText(ORCID_URL).first()).toBeVisible();
    await expect(viewPage.contributorNameDisplay(0)).not.toHaveText("Not available", { timeout: 10000 });
    await expect(viewPage.contributorNameDisplay(0)).not.toHaveText("Resolving…");
  });

  test("View page shows an existing ISNI contributor's identifier and resolved name", async ({
    page,
  }) => {
    const { formPage, contributorSection } = await setUpFormWithContributorRow(page);
    await saveWithIdentifier(page, contributorSection, formPage, MOCKED_ISNI_URL);

    // MOCKED_ISNI_URL resolves via the local mockserver's stubbed ISNI SRU
    // response (docker-compose/mockserver/expectations.json) to "Taylor
    // Swift" - but the shared branch-deployed CI environment resolves ISNI
    // through a different stub (IsniClientStub, hardcoded "Test User"), so
    // assert resolution succeeded rather than hardcoding either exact name,
    // mirroring the equivalent ORCID assertion above.
    const viewPage = new RaidViewPage(page);
    await expect(page.getByText(MOCKED_ISNI_URL).first()).toBeVisible();
    await expect(viewPage.contributorNameDisplay(0)).not.toHaveText("Not available", { timeout: 10000 });
    await expect(viewPage.contributorNameDisplay(0)).not.toHaveText("Resolving…");
  });

  test("View page falls back to a plain message when the ISNI name lookup fails", async ({
    page,
  }) => {
    const { formPage, contributorSection } = await setUpFormWithContributorRow(page);
    await saveWithIdentifier(page, contributorSection, formPage, MOCKED_ISNI_URL);
    await failIsniNameLookups(page);
    await page.reload();

    const viewPage = new RaidViewPage(page);
    await expect(page.getByText(MOCKED_ISNI_URL).first()).toBeVisible();
    await expect(viewPage.contributorNameDisplay(0)).toHaveText("Not available", { timeout: 10000 });
  });

  test("View page falls back to a plain message when the ORCID public API lookup fails", async ({
    page,
  }) => {
    const { formPage, contributorSection } = await setUpFormWithContributorRow(page);
    await failOrcidPublicApiLookups(page);
    await saveWithIdentifier(page, contributorSection, formPage, ORCID_URL);

    const viewPage = new RaidViewPage(page);
    await expect(page.getByText(ORCID_URL).first()).toBeVisible();
    await expect(viewPage.contributorNameDisplay(0)).toHaveText("Not available", { timeout: 10000 });
  });

  test("Edit page shows an existing ORCID contributor read-only with a resolved name, editable via the pencil icon", async ({
    page,
  }) => {
    const { formPage, contributorSection } = await setUpFormWithContributorRow(page);
    const [prefix, suffix] = await saveWithIdentifier(page, contributorSection, formPage, ORCID_URL);

    await formPage.goto(`/raids/${prefix}/${suffix}/edit`);

    const identifierDisplay = contributorSection.identifierDisplay(0);
    await expect(identifierDisplay).toContainText(ORCID_URL);
    await expect(identifierDisplay).not.toHaveText(`${ORCID_URL} — Not available`, { timeout: 10000 });
    await expect(page.locator('#contributor input[aria-label="search orcid"]')).toHaveCount(0);

    await contributorSection.clickEditIdentifier(0);
    await expect(page.locator('#contributor input[aria-label="search orcid"]')).toHaveValue(ORCID_URL);
  });

  test("Edit page still shows the identifier, without crashing, when the ORCID public API lookup fails", async ({
    page,
  }) => {
    const { formPage, contributorSection } = await setUpFormWithContributorRow(page);
    await failOrcidPublicApiLookups(page);
    const [prefix, suffix] = await saveWithIdentifier(page, contributorSection, formPage, ORCID_URL);

    await formPage.goto(`/raids/${prefix}/${suffix}/edit`);

    await expect(contributorSection.identifierDisplay(0)).toHaveText(
      `${ORCID_URL} — Not available`,
      { timeout: 10000 }
    );
  });

  test("Edit page still shows the identifier, without crashing, when the ISNI name lookup fails", async ({
    page,
  }) => {
    const { formPage, contributorSection } = await setUpFormWithContributorRow(page);
    await failIsniNameLookups(page);
    const [prefix, suffix] = await saveWithIdentifier(page, contributorSection, formPage, MOCKED_ISNI_URL);

    await formPage.goto(`/raids/${prefix}/${suffix}/edit`);

    await expect(contributorSection.identifierDisplay(0)).toHaveText(
      `${MOCKED_ISNI_URL} — Not available`,
      { timeout: 10000 }
    );
  });
});
