// RAID-800: E2E tests for paste-and-infer schemaUri recognition on the
// Related Object identifier field (Handle, RRID, ARK), plus regression
// guards for the existing DOI/unrecognised-URL behaviour.
//
// DOI, Handle (RAID-786) and RRID (RAID-787) all now have real backend
// validators (stubbed to avoid live external lookups in dev/local/CI — see
// ExternalPidService), so those tests assert actual save success. ARK
// (RAID-793) has no backend validator yet — POST /raid rejects any
// relatedObject with schemaUri "https://arks.org/" — so that test, and the
// unrecognised-URL regression guard (which never gets a schemaUri at all),
// still only assert on the outgoing request payload rather than save
// success.

import { test, expect } from "@playwright/test";
import { RaidFormPage } from "../page-objects/RaidFormPage";
import { TitleSection } from "../page-objects/sections/TitleSection";
import { DateSection } from "../page-objects/sections/DateSection";
import { AccessSection } from "../page-objects/sections/AccessSection";
import { ContributorSection } from "../page-objects/sections/ContributorSection";
import { RelatedObjectSection } from "../page-objects/sections/RelatedObjectSection";
import { validEmbargoExpiry } from "../utils/date-helpers";

const START_DATE = "2024-03-01";
const EMBARGOED_LABEL = "Embargoed Access";
const ACCESS_STATEMENT = "Embargoed for related-object inference e2e testing";
const EMBARGO_EXPIRY = validEmbargoExpiry();
const ORCID = "https://sandbox.orcid.org/0009-0002-5128-5184";

interface RelatedObjectPayload {
  relatedObject?: Array<{ id?: string; schemaUri?: string }>;
}

// Fills the minimal set of required fields so the form can be submitted,
// then adds one empty Related Object row ready to receive a pasted id.
async function setUpFormWithRelatedObjectRow(page: import("@playwright/test").Page) {
  const formPage = new RaidFormPage(page);
  const titleSection = new TitleSection(page);
  const dateSection = new DateSection(page);
  const accessSection = new AccessSection(page);
  const contributorSection = new ContributorSection(page);
  const relatedObjectSection = new RelatedObjectSection(page);

  await formPage.goto("/raids/new");
  await titleSection.fillText(0, `E2E Related Object Inference Test ${Date.now()}`);
  await dateSection.fillStartDate(START_DATE);
  await accessSection.selectAccessType(EMBARGOED_LABEL);
  await accessSection.fillStatementText(ACCESS_STATEMENT);
  await accessSection.fillEmbargoExpiry(EMBARGO_EXPIRY);
  await contributorSection.addItem();
  await contributorSection.searchAndSelectOrcid(0, ORCID);

  await relatedObjectSection.addItem();

  return { formPage, relatedObjectSection };
}

// Pastes the id, saves, and returns the relatedObject entry from the
// outgoing create-RAiD request body — regardless of whether the API
// ultimately accepts or rejects it. When `expectSaveSuccess` is set, also
// waits for the app to navigate to the saved RAiD's view page, i.e. asserts
// the API actually accepted the related object rather than just inspecting
// what was sent.
async function capturedSchemaUriFor(
  page: import("@playwright/test").Page,
  value: string,
  { expectSaveSuccess = false }: { expectSaveSuccess?: boolean } = {}
) {
  await page.context().grantPermissions(["clipboard-read", "clipboard-write"]);
  const { formPage, relatedObjectSection } = await setUpFormWithRelatedObjectRow(page);

  await relatedObjectSection.pasteId(0, value);

  const [request] = await Promise.all([
    page.waitForRequest(
      (req) => req.method() === "POST" && /\/raid\/?$/.test(new URL(req.url()).pathname)
    ),
    formPage.save(),
  ]);

  if (expectSaveSuccess) {
    await formPage.waitForSuccessfulSave();
  }

  const body = request.postDataJSON() as RelatedObjectPayload;
  return body.relatedObject?.[0];
}

test.describe("Related Object identifier paste-and-infer", { tag: "@local" }, () => {
  test("pasting a Handle URL infers the Handle schemaUri and saves successfully", async ({
    page,
  }) => {
    const relatedObject = await capturedSchemaUriFor(
      page,
      "https://hdl.handle.net/20.500.12345/abc123",
      { expectSaveSuccess: true }
    );
    expect(relatedObject?.schemaUri).toBe("https://hdl.handle.net/");
    expect(relatedObject?.id).toBe("https://hdl.handle.net/20.500.12345/abc123");
  });

  test("pasting an RRID URL infers the RRID schemaUri and saves successfully", async ({
    page,
  }) => {
    const relatedObject = await capturedSchemaUriFor(
      page,
      "https://scicrunch.org/resolver/RRID:AB_2298772",
      { expectSaveSuccess: true }
    );
    expect(relatedObject?.schemaUri).toBe("https://scicrunch.org/resolver/");
    expect(relatedObject?.id).toBe("https://scicrunch.org/resolver/RRID:AB_2298772");
  });

  // RAID-793 (ARK backend validator) hasn't landed — POST /raid rejects any
  // relatedObject with schemaUri "https://arks.org/", so this only asserts
  // what the frontend classifier sent, not that the API accepted it.
  test("pasting an ARK URL infers the ARK schemaUri, regardless of host", async ({
    page,
  }) => {
    const relatedObject = await capturedSchemaUriFor(
      page,
      "https://example-repository.edu/ark:/13030/kt6f59n8z3"
    );
    expect(relatedObject?.schemaUri).toBe("https://arks.org/");
    expect(relatedObject?.id).toBe(
      "https://example-repository.edu/ark:/13030/kt6f59n8z3"
    );
  });

  test("pasting a DOI URL still infers the DOI schemaUri and saves successfully (regression guard)", async ({
    page,
  }) => {
    const relatedObject = await capturedSchemaUriFor(
      page,
      "https://doi.org/10.5281/zenodo.1234567",
      { expectSaveSuccess: true }
    );
    expect(relatedObject?.schemaUri).toBe("https://doi.org/");
  });

  test("pasting an unrecognised URL leaves schemaUri uninferred (regression guard)", async ({
    page,
  }) => {
    const relatedObject = await capturedSchemaUriFor(
      page,
      "https://example.com/some-unrelated-path"
    );
    expect(relatedObject?.schemaUri || "").toBe("");
  });
});
