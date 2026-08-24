// RAID-480: e2e test for the "Create Service Point" form (operator-only).
//
// A service point created without a valid ROR previously stored blanks
// (whitespace/tabs) as its identifier owner, which silently broke minting
// for that service point in Demo and Prod. The fix added a required, format
// -checked `identifierOwner` field to the Zod validation schema (see
// createServicePointRequestValidationSchema in
// src/pages/service-point/validation/Rules.tsx), so a blank ROR is now
// rejected client-side before the request ever reaches the API.
//
// This test deliberately avoids driving the ROR lookup widget itself (it
// calls the external api.ror.org API - see the TODO in
// e2e/tests/03-optional-metadata.spec.ts for the existing precedent of not
// depending on that live API in e2e). Leaving the field untouched exercises
// the same "blank identifierOwner" defect class the ticket describes.
//
// Runs against the "operator" role (chromium-operator project) since the
// create form is only rendered for users with the `operator` realm role.

import { test, expect } from "@playwright/test";

test.describe("Service point creation: ROR is required", () => {
  test(
    "blocks submission when the Service Point Owner ROR field is left blank",
    { tag: "@local" },
    async ({ page }) => {
      await page.goto("/service-points");

      await page.getByRole("button", { name: /Create Service Point/ }).click();

      await page
        .getByLabel("Service point name *")
        .fill("E2E ROR Validation Test");
      await page.getByLabel("Admin email *").fill("e2e-admin@example.com");
      await page.getByLabel("Tech email *").fill("e2e-tech@example.com");
      // Deliberately leave the ROR ("Service Point Owner") field blank.

      await page.getByRole("button", { name: "Create" }).click();

      const dialog = page.getByRole("dialog");
      await expect(dialog).toBeVisible({ timeout: 5000 });
      await expect(dialog).toContainText("Identifier owner is required");

      // The form must not have submitted.
      await expect(
        page.getByText("Service point created successfully")
      ).toHaveCount(0);
    }
  );
});
