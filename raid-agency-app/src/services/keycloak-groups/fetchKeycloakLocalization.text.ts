import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { fetchKeycloakLocalization } from "./index";

describe("fetchKeycloakLocalization", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("calls the localization endpoint with URL-encoded params", async () => {
    const mockResponse = {
      ok: true,
      json: async () => ({ key: "welcomeMessage", value: "Welcome", locale: "en" }),
    };
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(mockResponse);

    await fetchKeycloakLocalization({
      token: "mock-token",
      key: "welcomeMessage",
      locale: "en",
    });

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("key=welcomeMessage&locale=en"),
      expect.objectContaining({
        headers: { Authorization: "Bearer mock-token" },
      })
    );
  });

  it("URL-encodes special characters in key and locale", async () => {
    const mockResponse = {
      ok: true,
      json: async () => ({ key: "a/b", value: "value", locale: "en-US" }),
    };
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(mockResponse);

    await fetchKeycloakLocalization({
      token: "mock-token",
      key: "a/b",
      locale: "en-US",
    });

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("key=a%2Fb&locale=en-US"),
      expect.anything()
    );
  });

  it("defaults locale to 'en' when not provided", async () => {
    const mockResponse = {
      ok: true,
      json: async () => ({ key: "welcomeMessage", value: "Welcome", locale: "en" }),
    };
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(mockResponse);

    await fetchKeycloakLocalization({
      token: "mock-token",
      key: "welcomeMessage",
    });

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("locale=en"),
      expect.anything()
    );
  });

  it("returns parsed JSON on success", async () => {
    const mockData = { key: "welcomeMessage", value: "Welcome", locale: "en" };
    const mockResponse = {
      ok: true,
      json: async () => mockData,
    };
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(mockResponse);

    const result = await fetchKeycloakLocalization({
      token: "mock-token",
      key: "welcomeMessage",
    });

    expect(result).toEqual(mockData);
  });

  it("throws an error when the response is not ok", async () => {
    const mockResponse = { ok: false };
    (fetch as ReturnType<typeof vi.fn>).mockResolvedValue(mockResponse);

    await expect(
      fetchKeycloakLocalization({ token: "mock-token", key: "missingKey" })
    ).rejects.toThrow("Failed to fetch localization");
  });
});
