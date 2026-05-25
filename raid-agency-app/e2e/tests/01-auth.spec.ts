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

        // Should redirect to Keycloak (/ → /login → Keycloak; allow 80s in CI).
        // Use "commit" so waitForURL returns as soon as the HTTP response headers
        // are received — Keycloak's full "domcontentloaded" or "load" events can
        // take 60+ seconds on a cold JVM, causing timeouts before the page renders.
        await page.waitForURL(keycloakUrlPattern, {
          timeout: 80_000,
          waitUntil: "commit",
        });
        await expect(page).toHaveURL(keycloakUrlPattern);

        // Do NOT assert #username visibility here — that requires Keycloak's JS
        // to load and render, which can take 60+ seconds on a cold JVM in CI.

        await context.close();
      }
    );

    test(
      "redirects to Keycloak login when visiting /raids unauthenticated",
      async ({ browser }) => {
        const context = await browser.newContext({ storageState: { cookies: [], origins: [] } });
        const page = await context.newPage();

        await page.goto("/raids", { waitUntil: "domcontentloaded" });

        // Should redirect to Keycloak (/raids → /login → Keycloak; allow 80s in CI).
        // Use "commit" — see above comment about cold JVM Keycloak page load times.
        await page.waitForURL(keycloakUrlPattern, {
          timeout: 80_000,
          waitUntil: "commit",
        });
        await expect(page).toHaveURL(keycloakUrlPattern);

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
        // Allow 90 s: the auth setup test warms Keycloak's prompt=none JVM code
        // path, so check-sso should complete in seconds — but the API response
        // can still be slow on a cold Spring Boot JVM in CI.
        await expect(
          page.getByRole("grid").or(
            page.getByText("RAiDs could not be fetched")
          )
        ).toBeVisible({ timeout: 90_000 });
      }
    );
  });
});
