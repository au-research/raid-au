// RAID-536: Global auth setup - runs once before all tests to authenticate
// and save session state to e2e/.auth/user.json

import { test as setup } from "@playwright/test";
import { fileURLToPath } from "url";
import path from "path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const authFile = path.join(__dirname, "../.auth/user.json");

const { VITE_KEYCLOAK_URL } = process.env;

if (!VITE_KEYCLOAK_URL) {
  throw new Error("VITE_KEYCLOAK_URL environment variable is not set.");
}

// Escape special regex characters so the URL can be used as a literal match
const keycloakUrlPattern = new RegExp(
  VITE_KEYCLOAK_URL.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
);

// The auth setup needs more time than ordinary tests: it boots the app,
// waits for Keycloak's (JVM) login page to render, logs in, and waits for
// the redirect back to the app — each step can be slow in CI on first run.
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

  // Navigate directly to the app login page. This skips the ProtectedRoute
  // check-sso delay and triggers keycloak.login() immediately, which redirects
  // the browser to Keycloak. Using /login instead of / avoids the silent-SSO
  // iframe round-trip that can stall CI environments.
  //
  // Use waitUntil:"domcontentloaded" so page.goto() returns as soon as the
  // /login HTML is parsed, before Keycloak JS fires the redirect. Without
  // this, goto() follows the client-side redirect to Keycloak and then waits
  // for the Keycloak page's full "load" event (all JS/CSS/fonts), which can
  // exceed 30 s on a cold JVM and eat into the remaining test time.
  await page.goto("/login", { waitUntil: "domcontentloaded" });

  // Wait for the browser to reach the Keycloak login URL.
  // The app boot → Keycloak init → login() redirect chain can take up to 60s
  // on a cold CI runner (JVM warmup on first Keycloak request).
  // Use "domcontentloaded" so we return as soon as the login form's HTML is
  // parsed — the #username field is in the initial HTML, so we don't need to
  // wait for every stylesheet and font Keycloak loads.
  await page.waitForURL(keycloakUrlPattern, {
    timeout: 60_000,
    waitUntil: "domcontentloaded",
  });

  // Wait for the Keycloak username field to be rendered
  await page.waitForSelector("#username", { timeout: 15_000 });

  // Fill in credentials on the Keycloak login page
  await page.locator("#username").fill(username);
  await page.locator("#password").fill(password);
  await page.locator('[type="submit"]').click();

  // Wait for redirect back to the app after login.
  // After Keycloak login the app lands on "/login?redirect=..." which then
  // navigates away — wait for the pathname to leave /login entirely.
  await page.waitForFunction(
    () => !window.location.pathname.startsWith("/login"),
    { timeout: 30_000 }
  );
  await page.waitForLoadState("networkidle");

  // Save authenticated session state
  await page.context().storageState({ path: authFile });
});
