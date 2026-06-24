import { describe, it, expect, beforeEach, vi } from "vitest";
import { RuntimeConfig } from "./RuntimeConfig";

const mockConfig: RuntimeConfig = {
  keycloak: { url: "http://localhost:8001", realm: "raid", clientId: "raid-api" },
  apiBaseUrl: "http://localhost:8080",
  environment: "dev",
  supportEmail: "contact@raid.org",
  googleAnalytics: {},
  services: {
    orcid: "https://orcid.test.raid.org.au",
    invite: "https://invite.test.raid.org.au",
    staticProd: "https://static.prod.raid.org.au",
    staticBase: "https://static.{env}.raid.org.au",
  },
  app: {
    orcid: {
      placeholder: "Enter ORCID iD (e.g., 0000-0002-1825-0097)",
      helpText: "Enter a valid ORCID iD",
    },
  },
};

// Reset the module before each test to clear the module-level _runtimeConfig variable
describe("RuntimeConfigContext module store", () => {
  beforeEach(() => {
    vi.resetModules();
  });

  it("getRuntimeConfig throws before setRuntimeConfig is called", async () => {
    const { getRuntimeConfig } = await import("./RuntimeConfigContext");

    expect(() => getRuntimeConfig()).toThrow(
      "[RuntimeConfig] Not initialized"
    );
  });

  it("getRuntimeConfig returns the config after setRuntimeConfig", async () => {
    const { setRuntimeConfig, getRuntimeConfig } = await import("./RuntimeConfigContext");

    setRuntimeConfig(mockConfig);

    expect(getRuntimeConfig()).toBe(mockConfig);
  });

  it("getRuntimeConfig returns the last config set", async () => {
    const { setRuntimeConfig, getRuntimeConfig } = await import("./RuntimeConfigContext");
    const updatedConfig = { ...mockConfig, environment: "prod" };

    setRuntimeConfig(mockConfig);
    setRuntimeConfig(updatedConfig);

    expect(getRuntimeConfig().environment).toBe("prod");
  });
});
