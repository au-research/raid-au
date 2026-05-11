import { describe, it, expect, vi, beforeEach } from "vitest";
import { loadConfig } from "./Configloader";

const mockValidConfig = {
  keycloak: { url: "http://localhost:8001", realm: "raid", clientId: "raid-api" },
  apiBaseUrl: "http://localhost:8080",
  environment: "dev",
  supportEmail: "contact@raid.org",
  googleAnalytics: { measurementId: "G-123", measurementIdDemo: "G-456" },
  services: {
    orcid: "https://orcid.test.raid.org.au",
    invite: "https://invite.test.raid.org.au",
    staticProd: "https://static.prod.raid.org.au",
    staticBase: "https://static.{env}.raid.org.au",
  },
};

const mockFetch = (body: unknown, ok = true, status = 200) =>
  vi.fn().mockResolvedValue({
    ok,
    status,
    statusText: ok ? "OK" : "Not Found",
    json: () => Promise.resolve(body),
  });

describe("loadConfig", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
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

    it("throws when the file cannot be fetched", async () => {
      vi.stubGlobal("fetch", mockFetch({}, false, 404));

      await expect(loadConfig()).rejects.toThrow(
        "[Config] Failed to load /app-config.json: 404"
      );
    });
  });

  describe("runtime config extraction", () => {
    it("extracts all runtime fields correctly", async () => {
      vi.stubGlobal("fetch", mockFetch(mockValidConfig));

      const { runtime } = await loadConfig();

      expect(runtime.keycloak).toEqual(mockValidConfig.keycloak);
      expect(runtime.apiBaseUrl).toBe("http://localhost:8080");
      expect(runtime.environment).toBe("dev");
      expect(runtime.supportEmail).toBe("contact@raid.org");
      expect(runtime.services).toEqual(mockValidConfig.services);
      expect(runtime.googleAnalytics).toEqual(mockValidConfig.googleAnalytics);
    });

    it("defaults googleAnalytics to empty object when absent", async () => {
      const { googleAnalytics: _, ...configWithoutGA } = mockValidConfig;
      vi.stubGlobal("fetch", mockFetch(configWithoutGA));

      const { runtime } = await loadConfig();

      expect(runtime.googleAnalytics).toEqual({});
    });
  });

  describe("validation", () => {
    it("throws when multiple required fields are missing", async () => {
      vi.stubGlobal("fetch", mockFetch({ environment: "dev" }));

      await expect(loadConfig()).rejects.toThrow(
        "[Config] Missing required fields in app-config.json"
      );
    });

    it("lists all missing fields in the error", async () => {
      vi.stubGlobal("fetch", mockFetch({ environment: "dev" }));

      await expect(loadConfig()).rejects.toThrow("keycloak.url");
    });

    it("throws when keycloak sub-fields are missing", async () => {
      vi.stubGlobal("fetch", mockFetch({ ...mockValidConfig, keycloak: { url: "http://localhost:8001" } }));

      await expect(loadConfig()).rejects.toThrow("keycloak.realm");
    });

    it("throws when a services sub-field is missing", async () => {
      const { services: { orcid: _, ...servicesWithoutOrcid }, ...rest } = mockValidConfig;
      vi.stubGlobal("fetch", mockFetch({ ...rest, services: servicesWithoutOrcid }));

      await expect(loadConfig()).rejects.toThrow("services.orcid");
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
