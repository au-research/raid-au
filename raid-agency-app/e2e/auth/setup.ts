// RAID-536: Global auth setup - runs once before all tests to authenticate
// and save session state to e2e/.auth/user.json
//
// Strategy: navigate DIRECTLY to Keycloak's authorization endpoint rather
// than going through the app's boot sequence (config fetch → React render →
// Keycloak JS init → silent SSO iframe). That chain depends on the app JS
// loading and Keycloak responding to a cold-JVM silent-SSO request, which
// can take 60 s+ in CI and makes the setup test inherently flaky.
//
// By going straight to Keycloak, we:
//   1. Fill in credentials on the Keycloak login form (fast — Keycloak just
//      needs to serve its own HTML, no app JS involved).
//   2. Let Keycloak set the SSO session cookie in the browser.
//   3. Navigate the app to /raids and wait for the page to fully render.
//      This does two things:
//      a) Ensures check-sso completes and localStorage tokens are saved.
//      b) Warms up Keycloak's prompt=none code path (a separate JVM code
//         path from the login form), so subsequent tests' check-sso runs
//         in seconds rather than 60 s+.

import { test as setup, expect } from "@playwright/test";
import { fileURLToPath } from "url";
import path from "path";
import crypto from "crypto";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const authFile = path.join(__dirname, "../.auth/user.json");

const {
  VITE_KEYCLOAK_URL,
  VITE_KEYCLOAK_REALM,
  VITE_KEYCLOAK_CLIENT_ID,
  BASE_URL,
} = process.env;

if (!VITE_KEYCLOAK_URL) {
  throw new Error("VITE_KEYCLOAK_URL environment variable is not set.");
}
if (!VITE_KEYCLOAK_REALM) {
  throw new Error("VITE_KEYCLOAK_REALM environment variable is not set.");
}
if (!VITE_KEYCLOAK_CLIENT_ID) {
  throw new Error("VITE_KEYCLOAK_CLIENT_ID environment variable is not set.");
}
if (!BASE_URL) {
  throw new Error("BASE_URL environment variable is not set.");
}

// 90-second budget:
//   ~30 s  direct Keycloak login (navigate to Keycloak, fill form, redirect back)
//   ~60 s  buffer (Keycloak JVM is pre-warmed by CI workflow step before this runs)
setup.setTimeout(90_000);

setup("authenticate", async ({ page }) => {
  const username = process.env.VITE_KEYCLOAK_E2E_USER;
  const password = process.env.VITE_KEYCLOAK_E2E_PASSWORD;

  if (!username) {
    throw new Error(
      "VITE_KEYCLOAK_E2E_USER environment variable is not set. " +
        "Add it to your .env file."
    );
  }
  if (!password) {
    throw new Error(
      "VITE_KEYCLOAK_E2E_PASSWORD environment variable is not set. " +
        "Add it to your .env file."
    );
  }

  // Generate PKCE code_verifier / code_challenge.
  // Keycloak 26 enforces PKCE for public clients, so we must supply one.
  const codeVerifier = crypto.randomBytes(32).toString("base64url");
  const codeChallenge = crypto
    .createHash("sha256")
    .update(codeVerifier)
    .digest("base64url");

  // Navigate directly to Keycloak's authorization endpoint.
  // redirect_uri must match one of the client's registered URIs.
  const keycloakAuthUrl =
    `${VITE_KEYCLOAK_URL}/realms/${VITE_KEYCLOAK_REALM}` +
    `/protocol/openid-connect/auth` +
    `?client_id=${VITE_KEYCLOAK_CLIENT_ID}` +
    `&redirect_uri=${encodeURIComponent(BASE_URL + "/")}` +
    `&response_type=code` +
    `&scope=openid` +
    `&code_challenge=${codeChallenge}` +
    `&code_challenge_method=S256`;

  // Use domcontentloaded — the #username field is in Keycloak's initial HTML,
  // so we don't need to wait for all of Keycloak's JS/CSS/fonts to load.
  await page.goto(keycloakAuthUrl, { waitUntil: "domcontentloaded" });

  // Wait for and fill in the Keycloak login form
  await page.waitForSelector("#username", { timeout: 30_000 });
  await page.locator("#username").fill(username);
  await page.locator("#password").fill(password);
  await page.locator('[type="submit"]').click();

  // After login Keycloak redirects to BASE_URL/?code=...&session_state=...
  // We only need the session cookie that Keycloak set — we don't need to
  // exchange the auth code ourselves.  Wait until the navigation to the app
  // commits (HTTP response headers received), then navigate away cleanly.
  await page.waitForURL((url) => url.href.startsWith(BASE_URL!), {
    timeout: 30_000,
    waitUntil: "commit",
  });

  // Save authenticated session state (cookies only).
  //
  // The Keycloak SSO session cookie (set on localhost:8001 during login above)
  // is what subsequent tests need. When a test loads with this storageState,
  // the app's check-sso fires a prompt=none iframe to Keycloak — the SSO
  // cookie is sent, Keycloak validates the session, and the app authenticates.
  //
  // We do NOT navigate to /raids here. The CI workflow runs a dedicated
  // "Warm up Keycloak JVM" step before Playwright starts, which performs a
  // full login+prompt=none flow via curl. By the time setup.ts runs, the
  // prompt=none JVM code path is already warm, so check-sso in subsequent
  // tests completes in seconds rather than 60+ s.
  await page.context().storageState({ path: authFile });
});
