import { describe, it, expect } from "vitest";
import { hasConfigIssues, isUrlField, validateConfig } from "./ConfigValidation";

const completeConfig = {
  keycloak: { url: "https://iam.example.org", realm: "raid", clientId: "raid-api" },
  apiBaseUrl: "https://api.example.org",
  environment: "demo",
  supportEmail: "support@example.org",
  services: {
    orcid: "https://orcid.example.org",
    staticProd: "https://static.prod.example.org",
    staticBase: "https://static.{env}.example.org",
  },
  app: { orcid: { placeholder: "Enter ORCID iD", helpText: "Help" } },
};

describe("validateConfig", () => {
  it("reports no issues for a complete config", () => {
    const result = validateConfig(completeConfig);

    expect(hasConfigIssues(result.blocking)).toBe(false);
    expect(hasConfigIssues(result.warning)).toBe(false);
  });

  it("treats an empty or whitespace string as missing", () => {
    const result = validateConfig({ ...completeConfig, environment: "  " });

    expect(result.blocking.missing).toEqual(["environment"]);
  });

  it("treats a non-string value as invalid", () => {
    const result = validateConfig({ ...completeConfig, supportEmail: 42 });

    expect(result.warning.invalid).toEqual(["supportEmail"]);
  });

  it("accepts the {env} placeholder in services.staticBase", () => {
    const result = validateConfig(completeConfig);

    expect(result.warning.invalid).not.toContain("services.staticBase");
  });

  it.each(["localhost:8080", "ftp://example.org", "not a url"])(
    "rejects %s as a URL",
    (value) => {
      const result = validateConfig({ ...completeConfig, apiBaseUrl: value });

      expect(result.blocking.invalid).toEqual(["apiBaseUrl"]);
    }
  );

  it("does not fail when a parent block is not an object", () => {
    const result = validateConfig({ ...completeConfig, keycloak: "https://iam.example.org" });

    expect(result.blocking.missing).toEqual(["keycloak.url", "keycloak.realm", "keycloak.clientId"]);
  });

  it("separates blocking fields from warning fields", () => {
    const result = validateConfig({});

    expect(result.blocking.missing).toEqual([
      "keycloak.url",
      "keycloak.realm",
      "keycloak.clientId",
      "apiBaseUrl",
      "environment",
    ]);
    expect(result.warning.missing).toEqual([
      "supportEmail",
      "services.orcid",
      "services.staticProd",
      "services.staticBase",
      "app.orcid.placeholder",
      "app.orcid.helpText",
    ]);
  });
});

describe("isUrlField", () => {
  it("is true for URL fields and false otherwise", () => {
    expect(isUrlField("apiBaseUrl")).toBe(true);
    expect(isUrlField("keycloak.realm")).toBe(false);
    expect(isUrlField("unknown.field")).toBe(false);
  });
});
