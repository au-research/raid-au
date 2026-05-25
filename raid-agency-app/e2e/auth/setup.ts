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
  await page.goto("/login");

  // Wait for the browser to reach the Keycloak login URL.
  // In CI the full chain (app boot → Keycloak redirect) can take up to 30s.
  await page.waitForURL(keycloakUrlPattern, { timeout: 30000 });

  // Wait for the Keycloak username field to be rendered
  await page.waitForSelector("#username", { timeout: 15000 });

  // Fill in credentials on the Keycloak login page
  await page.locator("#username").fill(username);
  await page.locator("#password").fill(password);
  await page.locator('[type="submit"]').click();

  // Wait for redirect back to the app after login.
  // After Keycloak login the app lands on "/login?redirect=..." which then
  // navigates away — wait for the pathname to leave /login entirely.
  await page.waitForFunction(
    () => !window.location.pathname.startsWith("/login"),
    { timeout: 20000 }
  );
  await page.waitForLoadState("networkidle");

  // Save authenticated session state
  await page.context().storageState({ path: authFile });
});
