// RAID-536: Global auth setup - runs once before all tests to authenticate
// and save session state to e2e/.auth/user.json

import { test as setup, expect } from "@playwright/test";
import { fileURLToPath } from "url";
import path from "path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const authFile = path.join(__dirname, "../.auth/user.json");

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

  // Navigate to the app - it will redirect to Keycloak
  await page.goto("/");

  // Wait for redirect to Keycloak login page
  await page.waitForSelector("#username", { timeout: 15000 });

  // Fill in credentials on the Keycloak login page
  await page.locator("#username").fill(username);
  await page.locator("#password").fill(password);
  await page.locator('[type="submit"]').click();

  // Wait for redirect back to the app after login.
  // After Keycloak login the app lands on "/" — wait for the login page to disappear.
  await page.waitForFunction(
    () => !window.location.pathname.startsWith("/login"),
    { timeout: 15000 }
  );
  await page.waitForLoadState("networkidle");

  // Save authenticated session state
  await page.context().storageState({ path: authFile });
});
