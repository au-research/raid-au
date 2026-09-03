// RAID-826: e2e test for the Service Point Admin self-service client
// credentials screen (create / view secret / rotate / revoke), against the
// real IAM backend endpoints from RAID-827.
//
// Runs against the "operator" role (chromium-operator project). The backend
// grants Operator a uniform short-circuit on these endpoints alongside the
// scoped "service-point-admin:<groupId>" role, so this covers the full
// lifecycle end-to-end without provisioning a dedicated scoped-admin e2e
// persona (which would need real Keycloak realm changes across
// environments - see the RAID-826 plan notes for that known coverage gap).
//
// Which service point to use is discovered from the running environment
// rather than hardcoded, following the same reasoning as RAID-856's fix to
// service-point-group-id-error.spec.ts: only a service point with a
// resolved (valid) group id renders the Client credentials tab at all.

import { test, expect, type Locator, type Page } from "@playwright/test";

function rowsWithResolvedGroup(page: Page): Locator {
  return page.locator('[role="row"][data-id]').filter({
    has: page.locator(
      '[data-field="groupId"] [data-testid="CheckCircleOutlineIcon"]'
    ),
  });
}

test.describe("Service point client credentials", () => {
  test(
    "supports create, view secret (masked/reveal/copy), rotate, and revoke",
    { tag: "@local" },
    async ({ page, context }) => {
      await context.grantPermissions(["clipboard-read", "clipboard-write"]);

      await page.goto("/service-points");
      await expect(page.getByRole("grid")).toBeVisible({ timeout: 15000 });

      const resolved = rowsWithResolvedGroup(page);
      const count = await resolved.count();
      test.skip(count < 1, "needs at least one service point with a resolved group");

      const servicePointId = await resolved.first().getAttribute("data-id");
      await page.goto(`/service-points/${servicePointId}`);

      const credentialsTab = page.getByRole("tab", { name: "Client credentials" });
      await expect(credentialsTab).toBeVisible({ timeout: 10000 });
      await credentialsTab.click();
      await expect(page).toHaveURL(/[?&]tab=credentials/);

      await page.getByText("Create client credential").click();
      const label = `e2e-cred-${Date.now()}`;
      await page.getByLabel("Label").fill(label);
      await page.getByRole("button", { name: "Create", exact: true }).click();

      const clientIdField = page.getByLabel("Client ID");
      const secretField = page.getByLabel("Secret");
      await expect(secretField).toBeVisible({ timeout: 10000 });

      // Masked by default.
      const maskedValue = await secretField.inputValue();
      expect(maskedValue).toMatch(/^•+$/);

      await page.getByRole("button", { name: "Reveal secret" }).click();
      const revealedSecret = await secretField.inputValue();
      expect(revealedSecret).not.toMatch(/^•+$/);
      expect(revealedSecret.length).toBeGreaterThan(0);

      await page.getByRole("button", { name: "Copy secret" }).click();
      const copiedSecret = await page.evaluate(() => navigator.clipboard.readText());
      expect(copiedSecret).toBe(revealedSecret);

      await page.getByRole("button", { name: "Reveal client id" }).click();
      const revealedClientId = await clientIdField.inputValue();
      expect(revealedClientId).not.toMatch(/^•+$/);

      await page.getByRole("button", { name: "Done" }).click();

      const row = page.getByRole("row").filter({ hasText: label });
      await expect(row).toBeVisible();
      await expect(row.getByText("Enabled")).toBeVisible();

      await row.getByLabel("rotate secret").click();
      await expect(page.getByText(/Secret rotated/i)).toBeVisible({ timeout: 10000 });
      const rotatedSecret = await secretField.inputValue();
      expect(rotatedSecret).toMatch(/^•+$/);
      await page.getByRole("button", { name: "Done" }).click();

      await row.getByLabel("revoke").click();
      const dialog = page.getByRole("dialog");
      await expect(dialog).toBeVisible();
      await dialog.getByRole("button", { name: "Revoke" }).click();

      await expect(row.getByText("Revoked")).toBeVisible({ timeout: 10000 });
      await expect(row.getByLabel("rotate secret")).toBeDisabled();
      await expect(row.getByLabel("revoke")).toBeDisabled();
    }
  );
});
