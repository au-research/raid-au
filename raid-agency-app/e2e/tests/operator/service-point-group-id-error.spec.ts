// RAID-659: e2e test for the operator "Manage service points" page.
//
// Before this fix, a service point whose Keycloak group could not be
// resolved (a dangling/invalid groupId) caused the *entire* list request to
// reject, replacing the whole table with a generic "Service point could not
// be fetched" error - even for service points whose groups were fine. The
// fix (see fetchServicePointsWithMembers in
// src/services/service-points/index.ts) catches the failure per service
// point and flags just that row via `groupIdError`, which
// ServicePointsTable/GroupIdCell renders as an "Group ID is invalid" icon.
//
// This test targets the seeded "raido" service point (group_id
// 169bd3f3-dd42-4ac0-b89a-fb49648e5eff - see
// api-svc/db/src/main/resources/db/env/dev/V32.1__update_service_point_repository.sql)
// and mocks only that group's Keycloak lookup to fail, leaving every other
// service point's lookup untouched, to prove the failure is now isolated.
//
// Runs against the "operator" role (chromium-operator project) since the
// operator table is only rendered for users with the `operator` realm role.

import { test, expect } from "@playwright/test";

const RAID_AU_GROUP_ID = "169bd3f3-dd42-4ac0-b89a-fb49648e5eff";

test.describe("Service points: invalid group ID handling", () => {
  test(
    "shows a graceful per-row error instead of breaking the whole table",
    { tag: "@local" },
    async ({ page }) => {
      await page.route(
        (url) =>
          url.pathname.endsWith("/group") &&
          url.searchParams.get("groupId") === RAID_AU_GROUP_ID,
        (route) => route.fulfill({ status: 404, body: "Group not found" })
      );

      await page.goto("/service-points");

      const grid = page.getByRole("grid");
      await expect(grid).toBeVisible({ timeout: 15000 });

      // The whole view must not have fallen back to the generic error state.
      await expect(
        page.getByText("Service point could not be fetched")
      ).toHaveCount(0);

      const affectedRow = page.getByRole("row").filter({ hasText: "raido" });
      await expect(affectedRow).toBeVisible();
      await expect(
        affectedRow.locator('[data-field="groupId"] [data-testid="ErrorOutlineIcon"]')
      ).toBeVisible();

      await affectedRow
        .locator('[data-field="groupId"] [data-testid="ErrorOutlineIcon"]')
        .hover();
      await expect(page.getByRole("tooltip")).toHaveText("Group ID is invalid");

      // Other, unaffected service points still render normally.
      const uqRow = page
        .getByRole("row")
        .filter({ hasText: "RAiD AU Test Registry 2" });
      await expect(uqRow).toBeVisible();
      await expect(
        uqRow.locator('[data-field="groupId"] [data-testid="CheckCircleOutlineIcon"]')
      ).toBeVisible();
    }
  );
});
