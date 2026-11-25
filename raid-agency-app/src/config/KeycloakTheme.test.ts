import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchLoginTheme, resolveStartupBranding } from "./KeycloakTheme";
import { neutralBranding } from "./DefaultConfig";
import type { RawBranding } from "./SanitiseBranding";

const keycloak = {
  url: "https://iam.example.org",
  realm: "my realm",
  clientId: "raid-web",
};

function jsonResponse(body: unknown, init: ResponseInit = {}): Response {
  return new Response(JSON.stringify(body), { status: 200, ...init });
}

function stubFetch(impl: () => Promise<Response>) {
  const fetchMock = vi.fn<(input: string) => Promise<Response>>(impl);
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("fetchLoginTheme", () => {
  it("asks the realm theme endpoint for the client, encoding both values", async () => {
    const fetchMock = stubFetch(async () => jsonResponse({ loginTheme: "ardc-branding" }));

    await fetchLoginTheme(keycloak);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0][0]).toBe(
      "https://iam.example.org/realms/my%20realm/theme?client_id=raid-web"
    );
  });

  it("returns the theme name", async () => {
    stubFetch(async () => jsonResponse({ loginTheme: "ardc-branding" }));
    expect(await fetchLoginTheme(keycloak)).toEqual({
      ok: true,
      loginTheme: "ardc-branding",
    });
  });

  it("treats a null theme as a success with no theme", async () => {
    stubFetch(async () => jsonResponse({ loginTheme: null }));
    expect(await fetchLoginTheme(keycloak)).toEqual({ ok: true, loginTheme: null });
  });

  it("treats raid-custom as a success", async () => {
    stubFetch(async () => jsonResponse({ loginTheme: "raid-custom" }));
    expect(await fetchLoginTheme(keycloak)).toEqual({
      ok: true,
      loginTheme: "raid-custom",
    });
  });

  it("fails on a network error without throwing", async () => {
    stubFetch(async () => {
      throw new TypeError("Failed to fetch");
    });
    expect(await fetchLoginTheme(keycloak)).toEqual({ ok: false });
  });

  it.each([404, 500, 503])("fails on HTTP %i", async (status) => {
    stubFetch(async () => jsonResponse({ loginTheme: "ardc-branding" }, { status }));
    expect(await fetchLoginTheme(keycloak)).toEqual({ ok: false });
  });

  it("fails when the body is not JSON", async () => {
    stubFetch(async () => new Response("<html></html>", { status: 200 }));
    expect(await fetchLoginTheme(keycloak)).toEqual({ ok: false });
  });

  it.each([
    ["an empty object", {}],
    ["a number", { loginTheme: 3 }],
    ["an array", []],
    ["null", null],
  ])("fails when the body is %s", async (_label, body) => {
    stubFetch(async () => jsonResponse(body));
    expect(await fetchLoginTheme(keycloak)).toEqual({ ok: false });
  });
});

describe("resolveStartupBranding", () => {
  const raw: RawBranding = {
    theme: {},
    header: { title: "Agency RAiD" },
    footer: {},
  };

  it("uses the configured branding for a custom theme", async () => {
    stubFetch(async () => jsonResponse({ loginTheme: "ardc-branding" }));
    const result = await resolveStartupBranding(raw, keycloak);
    expect(result.themeLookupFailed).toBe(false);
    expect(result.branding.header.title).toBe("Agency RAiD");
  });

  it("uses neutral branding for raid-custom with no failure", async () => {
    stubFetch(async () => jsonResponse({ loginTheme: "raid-custom" }));
    const result = await resolveStartupBranding(raw, keycloak);
    expect(result.themeLookupFailed).toBe(false);
    expect(result.branding).toEqual(neutralBranding);
  });

  it("uses neutral branding for a null theme with no failure", async () => {
    stubFetch(async () => jsonResponse({ loginTheme: null }));
    const result = await resolveStartupBranding(raw, keycloak);
    expect(result.themeLookupFailed).toBe(false);
    expect(result.branding).toEqual(neutralBranding);
  });

  it("falls back to neutral branding and flags the failure", async () => {
    stubFetch(async () => {
      throw new TypeError("Failed to fetch");
    });
    const result = await resolveStartupBranding(raw, keycloak);
    expect(result.themeLookupFailed).toBe(true);
    expect(result.branding).toEqual(neutralBranding);
  });
});
