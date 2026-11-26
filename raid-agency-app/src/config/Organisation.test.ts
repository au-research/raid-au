import { describe, expect, it } from "vitest";
import { readOrganisation } from "./Organisation";
import { validateConfig } from "./ConfigValidation";

const required = {
  keycloak: { url: "https://iam.example.org", realm: "raid", clientId: "raid-api" },
  apiBaseUrl: "https://api.example.org",
  environment: "demo",
  supportEmail: "support@example.org",
  services: {
    orcid: "https://orcid.example.org",
    staticProd: "https://static.example.org",
    staticBase: "https://static.{env}.example.org",
  },
  app: { orcid: { placeholder: "ORCID", helpText: "Help" } },
};

describe("readOrganisation", () => {
  it("returns an empty object when the section is left out", () => {
    expect(readOrganisation(undefined)).toEqual({});
    expect(readOrganisation(null)).toEqual({});
    expect(readOrganisation("ARDC")).toEqual({});
    expect(readOrganisation([])).toEqual({});
  });

  it("reads every documented field", () => {
    expect(
      readOrganisation({
        name: "Example Agency",
        url: "https://agency.example.org/",
        privacyPolicyUrl: "https://agency.example.org/privacy",
        termsUrl: "https://agency.example.org/terms",
        dataDeletionDays: 14,
      })
    ).toEqual({
      name: "Example Agency",
      url: "https://agency.example.org/",
      privacyPolicyUrl: "https://agency.example.org/privacy",
      termsUrl: "https://agency.example.org/terms",
      dataDeletionDays: 14,
    });
  });

  it("trims a link, so a stray space never reaches an href", () => {
    expect(readOrganisation({ privacyPolicyUrl: " https://agency.example.org/privacy " }).privacyPolicyUrl).toBe(
      "https://agency.example.org/privacy"
    );
  });

  it("drops blank names and trims the rest", () => {
    expect(readOrganisation({ name: "   " }).name).toBeUndefined();
    expect(readOrganisation({ name: "  Example Agency " }).name).toBe("Example Agency");
  });

  it.each([
    ["plain http", "http://agency.example.org/privacy"],
    ["a relative path", "/privacy"],
    ["javascript:", "javascript:alert(1)"],
    ["mailto:", "mailto:privacy@example.org"],
    ["not a URL", "privacy policy"],
    ["a number", 42],
  ])("drops a link that is %s", (_label, value) => {
    const org = readOrganisation({ url: value, privacyPolicyUrl: value, termsUrl: value });

    expect(org.url).toBeUndefined();
    expect(org.privacyPolicyUrl).toBeUndefined();
    expect(org.termsUrl).toBeUndefined();
  });

  it.each([
    ["zero", 0],
    ["negative", -7],
    ["fractional", 1.5],
    ["text", "7"],
    ["null", null],
  ])("drops dataDeletionDays that is %s", (_label, value) => {
    expect(readOrganisation({ dataDeletionDays: value }).dataDeletionDays).toBeUndefined();
  });
});

describe("validateConfig: organisation (RAID-1066)", () => {
  const warningsFor = (organisation: unknown) =>
    validateConfig({ ...required, organisation }).warning;

  it("accepts a file with no organisation section", () => {
    expect(validateConfig(required).warning).toEqual({ missing: [], invalid: [] });
  });

  it("accepts a complete, valid section", () => {
    expect(
      warningsFor({
        name: "Example Agency",
        url: "https://agency.example.org/",
        privacyPolicyUrl: "https://agency.example.org/privacy",
        termsUrl: "https://agency.example.org/terms",
        dataDeletionDays: 7,
      })
    ).toEqual({ missing: [], invalid: [] });
  });

  it("names the section when it is not an object", () => {
    expect(warningsFor("ARDC").invalid).toEqual(["organisation"]);
  });

  it("names each wrong field, never its value", () => {
    const issues = warningsFor({
      name: 5,
      url: "http://agency.example.org",
      privacyPolicyUrl: "nope",
      termsUrl: "javascript:alert(1)",
      dataDeletionDays: 0,
    });

    expect(issues.invalid).toEqual([
      "organisation.name",
      "organisation.url",
      "organisation.privacyPolicyUrl",
      "organisation.termsUrl",
      "organisation.dataDeletionDays",
    ]);
    expect(JSON.stringify(issues)).not.toContain("javascript");
  });

  it("ignores blank optional fields", () => {
    expect(warningsFor({ name: "", privacyPolicyUrl: " " })).toEqual({ missing: [], invalid: [] });
  });
});
