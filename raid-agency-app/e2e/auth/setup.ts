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
//   3. Navigate the app to /raids — the app's check-sso now resolves
//      immediately because Keycloak already has an active session, so no
//      cold-start delay.

import { test as setup } from "@playwright/test";
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

// 2-minute budget: PKCE login round-trip + app boot + check-sso
setup.setTimeout(120_000);

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

  // Build a PKCE code_verifier / code_challenge pair.
  // Keycloak 26 enforces PKCE for public clients, so we must supply one.
  // We don't need to exchange the code ourselves — we just need the SSO
  // session cookie that Keycloak sets after a successful login.
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
  // exchange the auth code, so wait just until the navigation commits (HTTP
  // response received) and then navigate away before the app JS runs.
  await page.waitForURL((url) => url.href.startsWith(BASE_URL!), {
    timeout: 30_000,
    waitUntil: "commit",
  });

  // Navigate to /raids cleanly (no ?code=... params).
  // The SSO session cookie is now set, so the app's check-sso iframe will
  // authenticate immediately — no cold-JVM wait needed.
  await page.goto(`${BASE_URL}/raids`, { waitUntil: "domcontentloaded" });

  // Wait for check-sso to resolve. If authentication succeeded the URL
  // stays at /raids; if it failed ProtectedRoute redirects to /login.
  // We use an inverse wait: if /login appears within 30 s we fail fast,
  // otherwise (timeout = stayed on /raids) we declare success.
  try {
    await page.waitForURL(
      (url) => url.pathname.startsWith("/login"),
      { timeout: 30_000 }
    );
    throw new Error(
      "Auth setup failed: app redirected to /login after check-sso. " +
        "The Keycloak SSO session may not have been established."
    );
  } catch (err) {
    if (err instanceof Error && err.message.startsWith("Auth setup failed")) {
      throw err;
    }
    // Timeout — we stayed on /raids. Authentication succeeded.
  }

  await page.waitForLoadState("networkidle");

  // Save authenticated session state (cookies + localStorage tokens)
  await page.context().storageState({ path: authFile });
});
