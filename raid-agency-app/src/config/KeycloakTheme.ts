// Looks up which login theme the Keycloak realm uses for this client, so the
// app knows whether to apply the branding in app-config.json (RAID-1065).
//
// The endpoint is the realm resource added by RAID-1073:
//   GET {keycloak.url}/realms/{realm}/theme?client_id={clientId}
//   -> {"loginTheme": "<name>"} or {"loginTheme": null}
//
// There is no timeout and no cache: the app waits for the answer on every page
// load. A failed lookup never throws. It returns { ok: false } and the app
// shows neutral branding with a banner.

import { Branding } from "./Appconfig";
import { RuntimeConfig } from "./RuntimeConfig";
import { resolveBranding } from "./ResolveBranding";
import { RawBranding } from "./SanitiseBranding";

export type ThemeLookup =
  | { ok: true; loginTheme: string | null }
  | { ok: false };

const FAILED: ThemeLookup = { ok: false };

export async function fetchLoginTheme(
  keycloak: RuntimeConfig["keycloak"]
): Promise<ThemeLookup> {
  const url =
    `${keycloak.url}/realms/${encodeURIComponent(keycloak.realm)}` +
    `/theme?client_id=${encodeURIComponent(keycloak.clientId)}`;

  let response: Response;
  try {
    response = await fetch(url);
  } catch (cause) {
    console.warn(`[Config] Could not reach the Keycloak theme endpoint: ${String(cause)}`);
    return FAILED;
  }

  if (!response.ok) {
    console.warn(`[Config] Keycloak theme endpoint answered ${response.status}`);
    return FAILED;
  }

  let body: unknown;
  try {
    body = await response.json();
  } catch {
    console.warn("[Config] Keycloak theme endpoint did not return JSON");
    return FAILED;
  }

  if (body === null || typeof body !== "object" || Array.isArray(body)) {
    console.warn("[Config] Keycloak theme endpoint returned an unexpected body");
    return FAILED;
  }

  const loginTheme = (body as Record<string, unknown>).loginTheme;
  if (loginTheme === null || typeof loginTheme === "string") {
    return { ok: true, loginTheme };
  }

  console.warn("[Config] Keycloak theme endpoint returned no loginTheme field");
  return FAILED;
}

/**
 * Looks up the login theme and resolves the branding the app renders with.
 * A failed lookup gives the neutral branding and sets themeLookupFailed so the
 * app can tell people why.
 */
export async function resolveStartupBranding(
  raw: RawBranding,
  keycloak: RuntimeConfig["keycloak"]
): Promise<{ branding: Branding; themeLookupFailed: boolean }> {
  const lookup = await fetchLoginTheme(keycloak);
  if (!lookup.ok) {
    return { branding: resolveBranding(raw, null), themeLookupFailed: true };
  }
  return {
    branding: resolveBranding(raw, lookup.loginTheme),
    themeLookupFailed: false,
  };
}
