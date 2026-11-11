// RAID-941: missing or invalid app-config.json is shown in a banner under the
// header instead of a blank page. Each test intercepts /app-config.json and
// patches the environment's real config, so it runs against any environment.

import { test, expect, Page } from "@playwright/test";

type Patch = (config: Record<string, any>) => void;

const patchAppConfig = async (page: Page, patch: Patch) => {
  await page.route("**/app-config.json", async (route) => {
    const response = await route.fetch();
    const config = await response.json();
    patch(config);
    await route.fulfill({ response, json: config });
  });
};

test.describe("Configuration issues", () => {
  test.describe("blocking", () => {
    test("names a missing required field under a minimal header", async ({ page }) => {
      await patchAppConfig(page, (config) => {
        delete config.keycloak.url;
      });

      await page.goto("/raids");

      const banner = page.getByRole("alert");
      await expect(banner).toContainText("This site is not configured correctly");
      await expect(banner).toContainText("keycloak.url");
      await expect(page.getByTestId("config-error-header")).toBeVisible();
      await expect(page.getByRole("button", { name: "Reload" })).toBeVisible();
    });

    test("says the configuration could not be loaded when the file is not JSON", async ({ page }) => {
      await page.route("**/app-config.json", (route) =>
        route.fulfill({ status: 200, contentType: "text/html", body: "<!doctype html><html></html>" })
      );

      await page.goto("/");

      await expect(page.getByRole("alert")).toContainText("configuration file could not be loaded");
    });
  });

  test.describe("warning", () => {
    test("shows the warning banner under the header for a signed-in user", async ({ page }) => {
      await patchAppConfig(page, (config) => {
        delete config.services.orcid;
      });

      await page.goto("/raids");

      const banner = page.getByRole("status").filter({ hasText: "Some configuration is missing" });
      await expect(banner).toContainText("services.orcid");
      await expect(page.getByTestId("app-nav-bar")).toBeVisible();
    });

    test("pauses the login page with a continue button", async ({ browser }) => {
      const context = await browser.newContext({ storageState: { cookies: [], origins: [] } });
      const page = await context.newPage();
      await patchAppConfig(page, (config) => {
        delete config.services.orcid;
      });

      await page.goto("/login");

      await expect(page.getByRole("status").filter({ hasText: "services.orcid" })).toBeVisible();
      await expect(page.getByRole("button", { name: "Continue to log in" })).toBeVisible();

      await context.close();
    });
  });
});
