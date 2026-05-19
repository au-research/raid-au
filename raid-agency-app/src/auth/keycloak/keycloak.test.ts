import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("./hooks/useAuthHelper", () => ({
  useAuthHelper: vi.fn(),
}));

vi.mock("keycloak-js", () => {
  const MockKeycloak = vi.fn(function (this: Record<string, unknown>, config: unknown) {
    this._config = config;
  });
  return { default: MockKeycloak };
});

describe("Keycloak lazy initialization", () => {
  beforeEach(() => {
    vi.resetModules();
  });

  it("getKeycloakInstance throws before initKeycloakInstance is called", async () => {
    const { getKeycloakInstance } = await import("./index");

    expect(() => getKeycloakInstance()).toThrow(
      "[Keycloak] Not initialized. Call initKeycloakInstance() before rendering."
    );
  });

  it("getKeycloakInstance returns the instance after initKeycloakInstance", async () => {
    const { initKeycloakInstance, getKeycloakInstance } = await import("./index");

    initKeycloakInstance({ url: "http://localhost:8001", realm: "raid", clientId: "raid-api" });

    expect(getKeycloakInstance()).toBeDefined();
  });

  it("initKeycloakInstance creates Keycloak with the provided config", async () => {
    const Keycloak = (await import("keycloak-js")).default as ReturnType<typeof vi.fn>;
    const { initKeycloakInstance } = await import("./index");
    const config = { url: "http://localhost:8001", realm: "raid", clientId: "raid-api" };

    initKeycloakInstance(config);

    expect(Keycloak).toHaveBeenCalledWith(config);
  });

  it("getKeycloakInstance always returns the same instance", async () => {
    const { initKeycloakInstance, getKeycloakInstance } = await import("./index");

    initKeycloakInstance({ url: "http://localhost:8001", realm: "raid", clientId: "raid-api" });

    expect(getKeycloakInstance()).toBe(getKeycloakInstance());
  });
});
