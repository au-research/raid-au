// RAID-536: Authentication flow e2e tests
// Tests that unauthenticated access redirects to Keycloak,
// and that authenticated users can access the application.

import { test, expect } from "@playwright/test";

test.describe("Authentication", () => {
  test.describe("Unauthenticated access", () => {
    test(
      "redirects to Keycloak login when visiting root unauthenticated",
      { tag: "@local" },
      async ({ browser }) => {
        // Create a fresh context with no stored session
        const context = await browser.newContext({ storageState: { cookies: [], origins: [] } });
        const page = await context.newPage();

        await page.goto("/");

        // Should redirect to Keycloak
        await page.waitForURL(/localhost:8001/, { timeout: 15000 });
        await expect(page).toHaveURL(/localhost:8001/);

        // Keycloak login page should be visible
        await expect(page.locator("#username")).toBeVisible();
        await expect(page.locator("#password")).toBeVisible();

        await context.close();
      }
    );

    test(
      "redirects to Keycloak login when visiting /raids unauthenticated",
      { tag: "@local" },
      async ({ browser }) => {
        const context = await browser.newContext({ storageState: { cookies: [], origins: [] } });
        const page = await context.newPage();

        await page.goto("/raids");

        await page.waitForURL(/localhost:8001/, { timeout: 15000 });
        await expect(page).toHaveURL(/localhost:8001/);
        await expect(page.locator("#username")).toBeVisible();

        await context.close();
      }
    );
  });

  test.describe("Authenticated access", () => {
    // These tests use the storageState set up by the setup project

    test(
      "authenticated session allows access to /raids",
      { tag: "@local" },
      async ({ page }) => {
        await page.goto("/raids");

        // Should NOT be redirected to Keycloak
        await expect(page).not.toHaveURL(/localhost:8001/);
        await expect(page).toHaveURL(/\/raids/);
      }
    );

    test(
      "authenticated user can see the RAiD list page",
      { tag: "@local" },
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
