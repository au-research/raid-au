import { describe, it, expect } from "vitest";
import { hasConfigIssues, isSafeLink, isUrlField, validateConfig } from "./ConfigValidation";

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
      "supportEmail",
    ]);
    expect(result.warning.missing).toEqual([
      "services.orcid",
      "services.staticProd",
      "services.staticBase",
      "app.orcid.placeholder",
      "app.orcid.helpText",
    ]);
  });
});

describe("validateConfig: supportEmail is required (RAID-1065)", () => {
  it("blocks the app when supportEmail is missing", () => {
    const withoutSupportEmail: Record<string, unknown> = { ...completeConfig };
    delete withoutSupportEmail.supportEmail;

    const result = validateConfig(withoutSupportEmail);

    expect(result.blocking.missing).toEqual(["supportEmail"]);
    expect(result.warning.missing).not.toContain("supportEmail");
  });

  it.each(["", "   "])("treats a blank supportEmail (%j) as missing", (value) => {
    const result = validateConfig({ ...completeConfig, supportEmail: value });

    expect(result.blocking.missing).toEqual(["supportEmail"]);
  });

  it("blocks the app when supportEmail is not text", () => {
    const result = validateConfig({ ...completeConfig, supportEmail: 42 });

    expect(result.blocking.invalid).toEqual(["supportEmail"]);
  });
});

describe("validateConfig: optional branding fields", () => {
  const withBranding = (branding: Record<string, unknown>) => ({ ...completeConfig, ...branding });

  it("reports nothing when every branding field is left out", () => {
    expect(hasConfigIssues(validateConfig(completeConfig).warning)).toBe(false);
  });

  it("accepts a fully configured, valid branding", () => {
    const result = validateConfig(
      withBranding({
        contactEmail: "contact@example.org",
        theme: { mode: "dark" },
        header: {
          title: "SURF RAiD",
          logo: { light: "/a.svg", dark: "/b.svg", alt: "SURF" },
          menu: "ardc",
          navLinks: [
            { label: "About", path: "https://example.org/about" },
            { label: "Help", path: "mailto:help@example.org" },
          ],
        },
        footer: {
          variant: "generic",
          links: [{ label: "Privacy", path: "https://example.org/privacy" }],
        },
      })
    );

    expect(hasConfigIssues(result.blocking)).toBe(false);
    expect(hasConfigIssues(result.warning)).toBe(false);
  });

  describe("text fields", () => {
    it.each(["contactEmail", "header.title", "header.logo.light", "header.logo.dark", "header.logo.alt"])(
      "rejects a non-text %s",
      (path) => {
        const [first, ...rest] = path.split(".");
        const value = rest.reduceRight<unknown>((inner, key) => ({ [key]: inner }), 42);
        const result = validateConfig(withBranding({ [first]: value }));

        expect(result.warning.invalid).toEqual([path]);
      }
    );

    it("ignores a blank optional text field instead of reporting it", () => {
      const result = validateConfig(withBranding({ contactEmail: "  ", header: { title: "" } }));

      expect(hasConfigIssues(result.warning)).toBe(false);
    });
  });

  describe("header.menu and footer.variant", () => {
    it.each(["none", "ardc"])("accepts header.menu %s", (menu) => {
      expect(hasConfigIssues(validateConfig(withBranding({ header: { menu } })).warning)).toBe(false);
    });

    it.each(["generic", "ardc"])("accepts footer.variant %s", (variant) => {
      expect(hasConfigIssues(validateConfig(withBranding({ footer: { variant } })).warning)).toBe(false);
    });

    it.each(["foo", "ARDC", 5, true, {}])("rejects header.menu %j", (menu) => {
      const result = validateConfig(withBranding({ header: { menu } }));

      expect(result.warning.invalid).toEqual(["header.menu"]);
    });

    it.each(["foo", "none", 5])("rejects footer.variant %j", (variant) => {
      const result = validateConfig(withBranding({ footer: { variant } }));

      expect(result.warning.invalid).toEqual(["footer.variant"]);
    });
  });

  describe.each(["header.navLinks", "footer.links"])("%s", (path) => {
    const [section, key] = path.split(".");
    const configWithLinks = (links: unknown) => withBranding({ [section]: { [key]: links } });

    it("accepts an empty list", () => {
      expect(hasConfigIssues(validateConfig(configWithLinks([])).warning)).toBe(false);
    });

    it("accepts https and mailto links", () => {
      const result = validateConfig(
        configWithLinks([
          { label: "A", path: "https://example.org/a" },
          { label: "B", path: "mailto:b@example.org" },
        ])
      );

      expect(hasConfigIssues(result.warning)).toBe(false);
    });

    it.each([
      ["javascript:", "javascript:alert(1)"],
      ["http:", "http://example.org"],
      ["data:", "data:text/html,hi"],
      ["a relative path", "/privacy"],
      ["text that is not an address", "not a link"],
    ])("rejects a link that uses %s", (_label, linkPath) => {
      const result = validateConfig(configWithLinks([{ label: "X", path: linkPath }]));

      expect(result.warning.invalid).toEqual([path]);
    });

    it("rejects the list when one link is bad, even if others are fine", () => {
      const result = validateConfig(
        configWithLinks([
          { label: "Good", path: "https://example.org" },
          { label: "Bad", path: "javascript:alert(1)" },
        ])
      );

      expect(result.warning.invalid).toEqual([path]);
    });

    it.each([
      ["a link with no label", { path: "https://example.org" }],
      ["a link with a blank label", { label: "  ", path: "https://example.org" }],
      ["a link with no path", { label: "X" }],
      ["a link that is not an object", "https://example.org"],
    ])("rejects %s", (_label, link) => {
      expect(validateConfig(configWithLinks([link])).warning.invalid).toEqual([path]);
    });

    it("rejects a value that is not a list", () => {
      expect(validateConfig(configWithLinks("https://example.org")).warning.invalid).toEqual([path]);
    });

    it("never puts the offending value in the issues", () => {
      const result = validateConfig(configWithLinks([{ label: "X", path: "javascript:steal()" }]));

      expect(JSON.stringify(result)).not.toContain("steal");
    });
  });

  describe("sections and logo", () => {
    it.each(["theme", "header", "footer"])("rejects %s when it is not an object", (section) => {
      const result = validateConfig(withBranding({ [section]: "oops" }));

      expect(result.warning.invalid).toEqual([section]);
    });

    it("rejects header.logo when it is not an object", () => {
      const result = validateConfig(withBranding({ header: { logo: "/a.svg" } }));

      expect(result.warning.invalid).toEqual(["header.logo"]);
    });
  });
});

describe("validateConfig: retired keys", () => {
  it("names the old branding wrapper as no longer used", () => {
    const result = validateConfig({ ...completeConfig, branding: { default: false } });

    expect(result.warning.retired).toEqual(["branding"]);
    expect(hasConfigIssues(result.warning)).toBe(true);
  });

  it("names an empty branding object too, because the key itself is retired", () => {
    expect(validateConfig({ ...completeConfig, branding: {} }).warning.retired).toEqual(["branding"]);
  });

  it.each([null, undefined])("does not report branding when it is %s", (value) => {
    const result = validateConfig({ ...completeConfig, branding: value });

    expect(result.warning.retired ?? []).toEqual([]);
  });

  it("reports nothing retired for a config in the new layout", () => {
    expect(validateConfig(completeConfig).warning.retired ?? []).toEqual([]);
  });

  it("never reports a retired key as a blocking issue", () => {
    expect(validateConfig({ ...completeConfig, branding: {} }).blocking.retired ?? []).toEqual([]);
  });
});

describe("isUrlField", () => {
  it("is true for URL fields and false otherwise", () => {
    expect(isUrlField("apiBaseUrl")).toBe(true);
    expect(isUrlField("keycloak.realm")).toBe(false);
    expect(isUrlField("unknown.field")).toBe(false);
  });

  it("is false for link list fields, which are not a single URL", () => {
    expect(isUrlField("header.navLinks")).toBe(false);
    expect(isUrlField("footer.links")).toBe(false);
  });
});

describe("isSafeLink", () => {
  it.each([
    { label: "A", path: "https://example.org" },
    { label: "A", path: "https://example.org/a?b=c#d" },
    { label: "A", path: "mailto:someone@example.org" },
    { label: "A", path: "https://example.org", contact: true },
  ])("accepts %j", (link) => {
    expect(isSafeLink(link)).toBe(true);
  });

  it.each([
    null,
    undefined,
    "https://example.org",
    42,
    [],
    {},
    { label: "A" },
    { path: "https://example.org" },
    { label: "A", path: "http://example.org" },
    { label: "A", path: "JavaScript:alert(1)" },
    { label: "A", path: " javascript:alert(1)" },
    { label: "A", path: "//example.org" },
    { label: "A", path: "" },
  ])("rejects %j", (link) => {
    expect(isSafeLink(link)).toBe(false);
  });
});
