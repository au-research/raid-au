import { describe, it, expect, vi, beforeEach } from "vitest";
import { loadConfig } from "./Configloader";
import { ConfigError } from "./ConfigValidation";

const mockValidConfig = {
  keycloak: { url: "http://localhost:8001", realm: "raid", clientId: "raid-api" },
  apiBaseUrl: "http://localhost:8080",
  environment: "dev",
  supportEmail: "contact@raid.org",
  googleAnalytics: { measurementId: "G-123", measurementIdDemo: "G-456" },
  services: {
    orcid: "https://orcid.test.raid.org.au",
    staticProd: "https://static.prod.raid.org.au",
    staticBase: "https://static.{env}.raid.org.au",
  },
  app: {
    orcid: { placeholder: "Enter ORCID iD", helpText: "e.g. 0000-0002-1825-0097" },
  },
};

const mockFetch = (body: unknown, ok = true, status = 200) =>
  vi.fn().mockResolvedValue({
    ok,
    status,
    statusText: ok ? "OK" : "Not Found",
    json: () => Promise.resolve(body),
  });

const loadError = async (): Promise<ConfigError> => {
  try {
    await loadConfig();
  } catch (error) {
    if (error instanceof ConfigError) return error;
    throw error;
  }
  throw new Error("expected loadConfig() to throw");
};

describe("loadConfig", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.spyOn(console, "warn").mockImplementation(() => {});
  });

  describe("fetch behaviour", () => {
    it("fetches /app-config.json with cache-busting header", async () => {
      const spy = mockFetch(mockValidConfig);
      vi.stubGlobal("fetch", spy);

      await loadConfig();

      expect(spy).toHaveBeenCalledWith("/app-config.json", {
        headers: { "Cache-Control": "no-cache" },
      });
    });

    it("throws a load-failed ConfigError when the file cannot be fetched", async () => {
      vi.stubGlobal("fetch", mockFetch({}, false, 404));

      const error = await loadError();

      expect(error.reason).toBe("load-failed");
      expect(error.message).toContain("[Config] Failed to load /app-config.json: 404");
    });

    it("throws a load-failed ConfigError on a network error", async () => {
      vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));

      expect((await loadError()).reason).toBe("load-failed");
    });

    it("throws a load-failed ConfigError when the body is not JSON (CloudFront index.html fallback)", async () => {
      vi.stubGlobal(
        "fetch",
        vi.fn().mockResolvedValue({
          ok: true,
          status: 200,
          statusText: "OK",
          json: () => Promise.reject(new SyntaxError("Unexpected token '<'")),
        })
      );

      expect((await loadError()).reason).toBe("load-failed");
    });

    it.each([null, [], "text", 42])("throws a load-failed ConfigError when the JSON is %j", async (body) => {
      vi.stubGlobal("fetch", mockFetch(body));

      expect((await loadError()).reason).toBe("load-failed");
    });
  });

  describe("runtime config extraction", () => {
    it("extracts all runtime fields correctly", async () => {
      vi.stubGlobal("fetch", mockFetch(mockValidConfig));

      const { runtime, issues } = await loadConfig();

      expect(runtime.keycloak).toEqual(mockValidConfig.keycloak);
      expect(runtime.apiBaseUrl).toBe("http://localhost:8080");
      expect(runtime.environment).toBe("dev");
      expect(runtime.supportEmail).toBe("contact@raid.org");
      expect(runtime.services).toEqual(mockValidConfig.services);
      expect(runtime.googleAnalytics).toEqual(mockValidConfig.googleAnalytics);
      expect(runtime.app.orcid).toEqual(mockValidConfig.app.orcid);
      expect(issues).toEqual({ missing: [], invalid: [] });
    });

    it("defaults googleAnalytics to empty object when absent", async () => {
      const { googleAnalytics: _, ...configWithoutGA } = mockValidConfig;
      vi.stubGlobal("fetch", mockFetch(configWithoutGA));

      const { runtime } = await loadConfig();

      expect(runtime.googleAnalytics).toEqual({});
    });

    it("defaults missing services and app.orcid blocks to empty objects", async () => {
      const { services: _s, app: _a, ...configWithoutBlocks } = mockValidConfig;
      vi.stubGlobal("fetch", mockFetch(configWithoutBlocks));

      const { runtime } = await loadConfig();

      expect(runtime.services).toEqual({});
      expect(runtime.app.orcid).toEqual({});
    });
  });

  describe("blocking fields", () => {
    it.each(["keycloak.url", "keycloak.realm", "keycloak.clientId", "apiBaseUrl", "environment"])(
      "throws an invalid-fields ConfigError naming %s when it is missing",
      async (path) => {
        const config = structuredClone(mockValidConfig) as Record<string, any>;
        const keys = path.split(".");
        const parent = keys.slice(0, -1).reduce((node, key) => node[key], config);
        delete parent[keys[keys.length - 1]];
        vi.stubGlobal("fetch", mockFetch(config));

        const error = await loadError();

        expect(error.reason).toBe("invalid-fields");
        expect(error.issues.missing).toEqual([path]);
      }
    );

    it("lists every missing blocking field", async () => {
      vi.stubGlobal("fetch", mockFetch({ environment: "dev" }));

      const error = await loadError();

      expect(error.issues.missing).toEqual([
        "keycloak.url",
        "keycloak.realm",
        "keycloak.clientId",
        "apiBaseUrl",
      ]);
    });

    it("reports a blocking URL that is not a valid URL as invalid", async () => {
      vi.stubGlobal("fetch", mockFetch({ ...mockValidConfig, apiBaseUrl: "localhost:8080" }));

      const error = await loadError();

      expect(error.issues).toEqual({ missing: [], invalid: ["apiBaseUrl"] });
    });

    it("never includes field values in the error message", async () => {
      vi.stubGlobal(
        "fetch",
        mockFetch({ ...mockValidConfig, apiBaseUrl: "not-a-url-secret-value" })
      );

      const error = await loadError();

      expect(error.message).toContain("apiBaseUrl");
      expect(error.message).not.toContain("not-a-url-secret-value");
    });
  });

  describe("warning fields", () => {
    it("returns missing warning fields instead of throwing", async () => {
      const { services: { orcid: _, ...servicesWithoutOrcid }, ...rest } = mockValidConfig;
      vi.stubGlobal("fetch", mockFetch({ ...rest, services: servicesWithoutOrcid }));

      const { issues } = await loadConfig();

      expect(issues).toEqual({ missing: ["services.orcid"], invalid: [] });
    });

    it("returns missing ORCID text fields when the app block is absent", async () => {
      const { app: _, ...configWithoutApp } = mockValidConfig;
      vi.stubGlobal("fetch", mockFetch(configWithoutApp));

      const { issues } = await loadConfig();

      expect(issues.missing).toEqual(["app.orcid.placeholder", "app.orcid.helpText"]);
    });

    it("returns invalid warning URLs", async () => {
      vi.stubGlobal(
        "fetch",
        mockFetch({ ...mockValidConfig, services: { ...mockValidConfig.services, staticProd: "ftp://x" } })
      );

      const { issues } = await loadConfig();

      expect(issues).toEqual({ missing: [], invalid: ["services.staticProd"] });
    });
  });

  describe("branding config", () => {
    it("uses defaultConfig branding when branding section is absent", async () => {
      vi.stubGlobal("fetch", mockFetch(mockValidConfig));

      const { app } = await loadConfig();

      expect(app.header.title).toBe("RAiD");
      expect(app.header.logo).toBeDefined();
    });

    it("deep-merges branding overrides with defaults", async () => {
      vi.stubGlobal("fetch", mockFetch({
        ...mockValidConfig,
        branding: { header: { title: "Custom Title" } },
      }));

      const { app } = await loadConfig();

      expect(app.header.title).toBe("Custom Title");
      expect(app.header.logo).toBeDefined();
    });

    it("does not override defaults with undefined branding fields", async () => {
      vi.stubGlobal("fetch", mockFetch({
        ...mockValidConfig,
        branding: { theme: { mode: "dark" } },
      }));

      const { app } = await loadConfig();

      expect(app.theme.mode).toBe("dark");
      expect(app.header.title).toBe("RAiD");
    });
  });
});
