// RAID-608 / HELP-2844: a user elevated to the flat `group-admin` role, but
// never actually approved (granted service-point-user) for a given service
// point, was able to approve access requests for that service point via the
// notification centre despite never being explicitly granted access. Fixed
// in iam/src/main/java/au/org/raid/iam/provider/group/GroupController.java
// (isGroupAdminOf's flat fallback now requires isApprovedGroupMember, not
// just group membership) - see GroupServicePointAdminIntegrationTest.java
// for the backend-level coverage this e2e test complements.
//
// Uses two dedicated fixture users (see iam/doc/keycloak-configuration.md):
//   - raid-au-unapproved-admin: group-admin role, raw self-joined member of
//     the raid-au group, never granted service-point-user for it.
//   - raid-au-pending-user: raw self-joined member of raid-au with no roles,
//     i.e. a colleague's outstanding access request.
//
// Runs against the dedicated unapproved-admin session (chromium-unapproved-
// -admin project).

import { test, expect } from "@playwright/test";

const PENDING_USERNAME = "raid-au-pending-user";

test.describe("Service point admin authorization: unapproved group-admin", () => {
  test(
    "cannot approve a pending access request for a service point they are not an approved member of",
    { tag: "@local" },
    async ({ page }) => {
      await page.goto("/");

      // The notification bell (AppNavBar) polls for pending access requests
      // for any service point the signed-in group-admin administers.
      const bell = page.getByRole("button").filter({
        has: page.getByTestId("NotificationsIcon"),
      });
      await bell.click();

      // Notification groups render as collapsed accordions; expand the
      // (only) one so its pending-member list items actually become visible.
      // Scoped to the notification Drawer (MUI portals it to document.body)
      // so this doesn't accidentally match an unrelated collapsed control
      // elsewhere on the page, e.g. a header dropdown.
      const drawer = page.locator(".MuiDrawer-root");
      const accordionToggle = drawer.locator('[aria-expanded="false"]').first();
      await accordionToggle.waitFor({ timeout: 15000 });
      await accordionToggle.click();

      const pendingItem = page.getByRole("listitem").filter({
        hasText: PENDING_USERNAME,
      });
      await expect(pendingItem).toBeVisible({ timeout: 15000 });

      const approveButton = pendingItem.getByRole("button", { name: "Approve" });

      const grantResponse = page.waitForResponse(
        (response) =>
          response.url().endsWith("/group/grant") && response.request().method() === "PUT"
      );
      await approveButton.click();
      const response = await grantResponse;

      // The backend must reject the approval - the admin has no approved
      // membership of raid-au, only a flat group-admin role. JAX-RS's
      // NotAuthorizedException maps to 401 (see GroupController.grant()),
      // but assert loosely against 401/403 to match the existing backend
      // test convention (GroupServicePointAdminIntegrationTest#assertDenied).
      expect([401, 403]).toContain(response.status());

      await expect(page.getByRole("alert")).toContainText(
        "Failed to perform operation"
      );

      // The request must still be pending - it was not granted.
      await expect(pendingItem).toBeVisible();
    }
  );
});
