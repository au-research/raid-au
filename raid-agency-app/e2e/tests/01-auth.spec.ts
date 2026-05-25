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

        // Use domcontentloaded so goto() returns as soon as the app HTML is
        // parsed, before Keycloak JS fires the redirect. Without this, goto()
        // follows the client-side redirect to Keycloak and waits for
        // Keycloak's "load" event (all JS/CSS/fonts) which can hit the 30s
        // navigation timeout on a cold JVM.
        await page.goto("/", { waitUntil: "domcontentloaded" });

        // Should redirect to Keycloak (/ → /login → Keycloak; allow 60s in CI).
        // Use domcontentloaded so waitForURL returns as soon as Keycloak's HTML
        // is parsed — without this it also waits for the full "load" event
        // (all Keycloak JS/CSS/fonts) which can time out on a cold JVM.
        await page.waitForURL(keycloakUrlPattern, {
          timeout: 60_000,
          waitUntil: "domcontentloaded",
        });
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

        await page.goto("/raids", { waitUntil: "domcontentloaded" });

        // Should redirect to Keycloak (/raids → /login → Keycloak; allow 60s in CI)
        await page.waitForURL(keycloakUrlPattern, {
          timeout: 60_000,
          waitUntil: "domcontentloaded",
        });
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

        // The RAiD list page should render past the Keycloak init / loading state.
        // Accept either:
        //  - DataGrid (user has a service point and the API returned data)
        //  - API error alert (user has no service point or the API rejected the
        //    request) — auth still succeeded; this is a data/config issue, not auth
        //
        // "No RAiDs found" does NOT exist in the component — the DataGrid renders
        // with no rows (MUI shows "No rows." internally) or an ErrorAlertComponent.
        //
        // Allow 60 s: check-sso round-trip + API response can be slow in CI.
        await expect(
          page.getByRole("grid").or(
            page.getByText("RAiDs could not be fetched")
          )
        ).toBeVisible({ timeout: 60_000 });
      }
    );
  });
});
