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
// The test breaks exactly one service point's group lookup and leaves every
// other one untouched, to prove the failure is now isolated to its own row.
//
// Which service point that is, and which group id to break, are discovered
// from the running environment rather than hardcoded (RAID-856). The pairing
// this test used to assume - the "raido" service point carrying group
// 169bd3f3... - is created by db/env/dev/V32.1__update_service_point_repository.sql,
// which only local dev applies. Branch and test environments run
// db/migration,db/env/test, where "raido" has no group at all and the group
// belongs to a "RAiD AU (branch-...)" service point created at deploy time,
// so the hardcoded version could only ever pass locally.
//
// Runs against the "operator" role (chromium-operator project) since the
// operator table is only rendered for users with the `operator` realm role.

import { test, expect, type Locator, type Page } from "@playwright/test";

/** Rows whose group resolved cleanly - the only ones whose lookup is worth breaking. */
function rowsWithResolvedGroup(page: Page): Locator {
  return page.locator('[role="row"][data-id]').filter({
    has: page.locator(
      '[data-field="groupId"] [data-testid="CheckCircleOutlineIcon"]'
    ),
  });
}

test.describe("Service points: invalid group ID handling", () => {
  test(
    "shows a graceful per-row error instead of breaking the whole table",
    { tag: "@local" },
    async ({ page }) => {
      await page.goto("/service-points");
      await expect(page.getByRole("grid")).toBeVisible({ timeout: 15000 });

      const resolved = rowsWithResolvedGroup(page);
      const resolvedCount = await resolved.count();

      // Isolation is only observable with a second, unaffected row to compare
      // against, so this needs two service points with working groups.
      test.skip(
        resolvedCount < 2,
        `needs two service points with resolvable groups, found ${resolvedCount}`
      );

      const affectedId = await resolved.nth(0).getAttribute("data-id");
      const controlId = await resolved.nth(1).getAttribute("data-id");

      // A valid group cell's tooltip is the group id itself, so the environment
      // tells us which group to break.
      await resolved
        .nth(0)
        .locator('[data-field="groupId"] [data-testid="CheckCircleOutlineIcon"]')
        .hover();
      const tooltip = page.getByRole("tooltip");
      await expect(tooltip).toBeVisible();
      const targetGroupId = (await tooltip.textContent())?.trim();
      expect(targetGroupId, "expected the valid group cell to expose its group id")
        .toBeTruthy();

      await page.route(
        (url) =>
          url.pathname.endsWith("/group") &&
          url.searchParams.get("groupId") === targetGroupId,
        (route) => route.fulfill({ status: 404, body: "Group not found" })
      );

      await page.reload();
      await expect(page.getByRole("grid")).toBeVisible({ timeout: 15000 });

      // The whole view must not have fallen back to the generic error state.
      await expect(
        page.getByText("Service point could not be fetched")
      ).toHaveCount(0);

      const affectedRow = page.locator(`[role="row"][data-id="${affectedId}"]`);
      const affectedIcon = affectedRow.locator(
        '[data-field="groupId"] [data-testid="ErrorOutlineIcon"]'
      );
      await expect(affectedIcon).toBeVisible();

      await affectedIcon.hover();
      await expect(page.getByRole("tooltip")).toHaveText("Group ID is invalid");

      // The other service point, whose group lookup was left alone, still
      // renders normally - the failure did not spread.
      const controlRow = page.locator(`[role="row"][data-id="${controlId}"]`);
      await expect(controlRow).toBeVisible();
      await expect(
        controlRow.locator(
          '[data-field="groupId"] [data-testid="CheckCircleOutlineIcon"]'
        )
      ).toBeVisible();
    }
  );
});
