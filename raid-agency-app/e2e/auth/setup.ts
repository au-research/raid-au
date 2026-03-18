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
  try {
    await page.waitForSelector("#username", { timeout: 15000 });
  } catch {
    // Debug: capture page state on failure
    const url = page.url();
    const title = await page.title();
    const html = await page.content();
    console.log(`[AUTH DEBUG] URL: ${url}`);
    console.log(`[AUTH DEBUG] Title: ${title}`);
    console.log(`[AUTH DEBUG] HTML (first 2000): ${html.substring(0, 2000)}`);
    await page.screenshot({ path: "e2e/auth-debug.png", fullPage: true });
    throw new Error(
      `Auth setup failed: #username not found after 15s. URL: ${url}, Title: ${title}`
    );
  }

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
