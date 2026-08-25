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
// This test deliberately types free text into the ROR field rather than
// leaving it completely empty. The underlying <input> also carries a native
// HTML `required` attribute (CustomizedInputBase's `required` prop), and a
// genuinely empty required input makes the browser's own constraint
// validation intercept the click before React/RHF/Zod ever see a submit -
// no dialog, no network call, nothing observable in the DOM (confirmed by
// stepping through this manually: a real click or Enter-key submit attempt
// on a blank required field never fires the form's submit event at all).
// Typing free text (without using the ROR search+select dropdown - see the
// TODO in e2e/tests/03-optional-metadata.spec.ts for why e2e avoids the live
// api.ror.org lookup) satisfies that native check while leaving the actual
// react-hook-form value unset, since CustomizedInputBase only calls
// setSelectedValue (and therefore updates the form) when a dropdown result
// is picked. That reproduces the exact RAID-480 defect class: a user typing
// something that looks like a value, which never actually becomes the
// identifierOwner sent to the API - and lets the submission reach Zod,
// which correctly blocks it and shows the "Identifier owner is required"
// dialog.
//
// Runs against the "operator" role (chromium-operator project) since the
// create form is only rendered for users with the `operator` realm role.

import { test, expect } from "@playwright/test";

test.describe("Service point creation: ROR is required", () => {
  test(
    "blocks submission when the ROR field was typed into but never selected from the lookup",
    { tag: "@local" },
    async ({ page }) => {
      await page.goto("/service-points");

      await page.getByRole("button", { name: /Create Service Point/ }).click();

      await page
        .getByLabel("Service point name *")
        .fill("E2E ROR Validation Test");
      await page.getByLabel("Admin email *").fill("e2e-admin@example.com");
      await page.getByLabel("Tech email *").fill("e2e-tech@example.com");
      // Typed, but never selected from the ROR search dropdown - the
      // identifierOwner form value stays unset despite this non-empty input.
      await page
        .getByLabel("Search Text or lookup ROR ID")
        .fill("not-a-real-ror-value");

      await page.getByRole("button", { name: "Create", exact: true }).click();

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
