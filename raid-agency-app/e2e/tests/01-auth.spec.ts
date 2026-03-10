// RAID-536: Authentication flow e2e tests
// Tests that unauthenticated access redirects to Keycloak,
// and that authenticated users can access the application.

import { test, expect } from "@playwright/test";

const { VITE_KEYCLOAK_URL } = process.env;

if (!VITE_KEYCLOAK_URL) {
  throw new Error("VITE_KEYCLOAK_URL environment variable is not set.");
}

// Escape special regex characters in the URL so it can be used as a literal match
const keycloakUrlPattern = new RegExp(
  VITE_KEYCLOAK_URL.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
);

test.describe("Authentication", () => {
  test.describe("Unauthenticated access", () => {
    test(
      "redirects to Keycloak login when visiting root unauthenticated",
      async ({ browser }) => {
        // Create a fresh context with no stored session
        const context = await browser.newContext({ storageState: { cookies: [], origins: [] } });
        const page = await context.newPage();

        await page.goto("/");

        // Should redirect to Keycloak
        await page.waitForURL(keycloakUrlPattern, { timeout: 15000 });
        await expect(page).toHaveURL(keycloakUrlPattern);

        // Keycloak login page should be visible
        await expect(page.locator("#username")).toBeVisible();
        await expect(page.locator("#password")).toBeVisible();

        await context.close();
      }
    );

    test(
      "redirects to Keycloak login when visiting /raids unauthenticated",
      async ({ browser }) => {
        const context = await browser.newContext({ storageState: { cookies: [], origins: [] } });
        const page = await context.newPage();

        await page.goto("/raids");

        await page.waitForURL(keycloakUrlPattern, { timeout: 15000 });
        await expect(page).toHaveURL(keycloakUrlPattern);
        await expect(page.locator("#username")).toBeVisible();

        await context.close();
      }
    );
  });

  test.describe("Authenticated access", () => {
    // These tests use the storageState set up by the setup project

    test(
      "authenticated session allows access to /raids",
      async ({ page }) => {
        await page.goto("/raids");

        // Should NOT be redirected to Keycloak
        await expect(page).not.toHaveURL(keycloakUrlPattern);
        await expect(page).toHaveURL(/\/raids/);
      }
    );

    test(
      "authenticated user can see the RAiD list page",
      async ({ page }) => {
        await page.goto("/raids");

        // Should NOT be on the Keycloak login page
        await expect(page.locator("#username")).not.toBeVisible();

        // The RAiD list page should render — either a data grid or an empty state
        await expect(
          page.getByRole("grid").or(page.getByText("No RAiDs found"))
        ).toBeVisible({ timeout: 15000 });
      }
    );
  });
});
