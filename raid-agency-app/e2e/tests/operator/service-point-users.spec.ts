// RAID-919: e2e tests for a service point's Users list.
//
// Since admin rights became specific to each service point (RAID-884), the
// list showed every realm role a person holds, including one
// "service-point-admin:<groupId>" per service point they administer, and its
// admin buttons read the flat group-admin role, so an admin of another
// service point looked like an admin of this one. The list now shows plain
// statuses (Service Point User, Pending approval, Service Point Admin) for
// this service point only, hides operators, filters by status and puts each
// person's actions in a menu, with removal confirmed first.
//
// The Keycloak group endpoints are mocked (see
// e2e/utils/service-point-members-mock.ts) so each test controls exactly who
// is in the service point and can check every action's request without
// changing real users. The service point itself, the page and the operator
// session are real.
//
// Runs against the "operator" role (chromium-operator project): operators can
// open the Users tab of any service point with a group.

import { test, expect, type Locator, type Page } from "@playwright/test";
import {
  manyPeople,
  mockServicePointMembers,
  type MockPerson,
} from "../../utils/service-point-members-mock";

const PEOPLE: MockPerson[] = [
  {
    id: "avery",
    firstName: "Avery",
    lastName: "Lin",
    approved: true,
    adminHere: true,
    adminElsewhere: ["other-sp-1", "other-sp-2", "other-sp-3"],
  },
  { id: "sam", firstName: "Sam", lastName: "Okafor", approved: true },
  { id: "priya", firstName: "Priya", lastName: "Natarajan", approved: false },
  // Administers a different service point only.
  { id: "jordan", firstName: "Jordan", lastName: "Ellis", approved: true, adminElsewhere: ["other-sp-1"] },
  // Operators are not listed.
  { id: "riley", firstName: "Riley", lastName: "Operations", approved: true, operator: true },
];

function rowsWithResolvedGroup(page: Page): Locator {
  return page.locator('[role="row"][data-id]').filter({
    has: page.locator('[data-field="groupId"] [data-testid="CheckCircleOutlineIcon"]'),
  });
}

/** Opens the Users tab of a service point that has a group, found from the running environment. */
async function openUsersTab(page: Page) {
  await page.goto("/service-points");
  await expect(page.getByRole("grid")).toBeVisible({ timeout: 15000 });

  const resolved = rowsWithResolvedGroup(page);
  test.skip((await resolved.count()) < 1, "needs at least one service point with a group");
  const servicePointId = await resolved.first().getAttribute("data-id");

  await page.goto(`/service-points/${servicePointId}?tab=users`);
  const list = usersList(page);
  await expect(list).toBeVisible({ timeout: 15000 });
  return list;
}

const usersList = (page: Page) => page.getByRole("list", { name: /people$/ });

const rowFor = (page: Page, name: string) =>
  usersList(page).getByRole("listitem").filter({ hasText: name });

const filterButton = (page: Page, label: string, count?: number) =>
  page.getByRole("button", { name: new RegExp(`^${label}\\s*${count ?? "\\d+"}$`) });

const isScrollable = (locator: Locator) =>
  locator.evaluate((el) => el.scrollHeight > el.clientHeight);

/** Whether the scrolling panel around the list's own scroller (the page's content panel) scrolls. */
const isPanelAroundScrollable = (list: Locator) =>
  list.evaluate((el) => {
    let panel = el.parentElement?.parentElement ?? null;
    while (panel && getComputedStyle(panel).overflowY !== "auto") panel = panel.parentElement;
    return panel ? panel.scrollHeight > panel.clientHeight : false;
  });

async function openMenuFor(page: Page, name: string) {
  await page.getByRole("button", { name: `Actions for ${name}` }).click();
  const menu = page.getByRole("menu");
  await expect(menu).toBeVisible();
  return menu;
}

test.describe("Service point Users list", () => {
  test(
    "shows each person's status for this service point instead of raw role names",
    { tag: "@local" },
    async ({ page }) => {
      await mockServicePointMembers(page, PEOPLE);
      const list = await openUsersTab(page);

      await expect(list).not.toContainText("service-point-admin:");
      await expect(list).not.toContainText("group-admin");
      await expect(list).not.toContainText("default-roles-raid");

      const avery = rowFor(page, "Avery Lin");
      await expect(avery.getByText("Service Point User", { exact: true })).toBeVisible();
      await expect(avery.getByText("Service Point Admin", { exact: true })).toBeVisible();
      await expect(avery).toContainText("avery.lin · avery.lin@example.edu.au");

      await expect(rowFor(page, "Priya Natarajan").getByText("Pending approval")).toBeVisible();

      // An admin of another service point is not an admin of this one.
      const jordan = rowFor(page, "Jordan Ellis");
      await expect(jordan.getByText("Service Point User", { exact: true })).toBeVisible();
      await expect(jordan.getByText("Service Point Admin", { exact: true })).toHaveCount(0);

      await expect(page.getByText("Person", { exact: true })).toBeVisible();
      await expect(page.getByText("Actions", { exact: true })).toBeVisible();
    }
  );

  test("does not list or count operators", { tag: "@local" }, async ({ page }) => {
    await mockServicePointMembers(page, PEOPLE);
    const list = await openUsersTab(page);

    await expect(list.getByRole("listitem")).toHaveCount(4);
    await expect(list).not.toContainText("Riley Operations");
    await expect(list).not.toContainText("Operator");
    await expect(filterButton(page, "All", 4)).toBeVisible();
  });

  test("lists pending requests first, then everyone else by name", { tag: "@local" }, async ({ page }) => {
    await mockServicePointMembers(page, PEOPLE);
    const list = await openUsersTab(page);

    const items = list.getByRole("listitem");
    await expect(items.nth(0)).toContainText("Priya Natarajan");
    await expect(items.nth(1)).toContainText("Avery Lin");
    await expect(items.nth(2)).toContainText("Jordan Ellis");
    await expect(items.nth(3)).toContainText("Sam Okafor");
  });

  test("filters by status, with counts", { tag: "@local" }, async ({ page }) => {
    await mockServicePointMembers(page, PEOPLE);
    const list = await openUsersTab(page);

    await expect(filterButton(page, "All", 4)).toHaveAttribute("aria-pressed", "true");

    await filterButton(page, "Service Point Users", 3).click();
    await expect(usersList(page).getByRole("listitem")).toHaveCount(3);
    await expect(usersList(page)).not.toContainText("Priya Natarajan");

    await filterButton(page, "Pending approval", 1).click();
    await expect(usersList(page).getByRole("listitem")).toHaveCount(1);
    await expect(usersList(page)).toContainText("Priya Natarajan");

    await filterButton(page, "Service Point Admins", 1).click();
    await expect(usersList(page).getByRole("listitem")).toHaveCount(1);
    await expect(usersList(page)).toContainText("Avery Lin");

    await filterButton(page, "All", 4).click();
    await expect(list.getByRole("listitem")).toHaveCount(4);
  });

  test("says so when a filter matches nobody", { tag: "@local" }, async ({ page }) => {
    await mockServicePointMembers(page, PEOPLE.filter((person) => person.approved));
    await openUsersTab(page);

    await filterButton(page, "Pending approval", 0).click();
    await expect(page.getByText("No requests waiting for approval.")).toBeVisible();
  });

  test(
    "approves a pending request, and only then offers Make Service Point Admin",
    { tag: "@local" },
    async ({ page }) => {
      const mock = await mockServicePointMembers(page, PEOPLE);
      await openUsersTab(page);

      const menu = await openMenuFor(page, "Priya Natarajan");
      await expect(menu.getByRole("menuitem")).toHaveText([
        "Approve as Service Point User",
        "Remove from service point…",
      ]);

      await menu.getByRole("menuitem", { name: "Approve as Service Point User" }).click();

      await expect(page.getByText("Approved as Service Point User")).toBeVisible();
      expect(mock.callsTo("PUT", "grant")).toHaveLength(1);
      expect(mock.callsTo("PUT", "grant")[0].body).toMatchObject({ userId: "priya" });
      expect(mock.callsTo("PUT", "grant")[0].body?.groupId).toBeTruthy();

      const priya = rowFor(page, "Priya Natarajan");
      await expect(priya.getByText("Service Point User", { exact: true })).toBeVisible();
      await expect(filterButton(page, "Pending approval", 0)).toBeVisible();

      const after = await openMenuFor(page, "Priya Natarajan");
      await expect(after.getByRole("menuitem", { name: "Make Service Point Admin" })).toBeVisible();
    }
  );

  test("revokes a Service Point User", { tag: "@local" }, async ({ page }) => {
    const mock = await mockServicePointMembers(page, PEOPLE);
    await openUsersTab(page);

    const menu = await openMenuFor(page, "Sam Okafor");
    await expect(menu.getByRole("menuitem")).toHaveText([
      "Revoke Service Point User",
      "Make Service Point Admin",
      "Remove from service point…",
    ]);
    await menu.getByRole("menuitem", { name: "Revoke Service Point User" }).click();

    await expect(page.getByText("Revoked Service Point User")).toBeVisible();
    expect(mock.callsTo("PUT", "revoke")[0].body).toMatchObject({ userId: "sam" });
    await expect(rowFor(page, "Sam Okafor").getByText("Pending approval")).toBeVisible();
  });

  test(
    "makes an admin of another service point a Service Point Admin here",
    { tag: "@local" },
    async ({ page }) => {
      const mock = await mockServicePointMembers(page, PEOPLE);
      await openUsersTab(page);

      const menu = await openMenuFor(page, "Jordan Ellis");
      await menu.getByRole("menuitem", { name: "Make Service Point Admin" }).click();

      await expect(page.getByText("Made Service Point Admin")).toBeVisible();
      expect(mock.callsTo("PUT", "group-admin")[0].body).toMatchObject({ userId: "jordan" });
      await expect(
        rowFor(page, "Jordan Ellis").getByText("Service Point Admin", { exact: true })
      ).toBeVisible();
      await expect(filterButton(page, "Service Point Admins", 2)).toBeVisible();
    }
  );

  test("removes a Service Point Admin", { tag: "@local" }, async ({ page }) => {
    const mock = await mockServicePointMembers(page, PEOPLE);
    await openUsersTab(page);

    const menu = await openMenuFor(page, "Avery Lin");
    await expect(menu.getByRole("menuitem")).toHaveText([
      "Revoke Service Point User",
      "Remove Service Point Admin",
      "Remove from service point…",
    ]);
    await menu.getByRole("menuitem", { name: "Remove Service Point Admin" }).click();

    await expect(page.getByText("Removed Service Point Admin")).toBeVisible();
    expect(mock.callsTo("DELETE", "group-admin")[0].body).toMatchObject({ userId: "avery" });
    await expect(
      rowFor(page, "Avery Lin").getByText("Service Point Admin", { exact: true })
    ).toHaveCount(0);
  });

  test(
    "asks for confirmation before removing someone from the service point",
    { tag: "@local" },
    async ({ page }) => {
      const mock = await mockServicePointMembers(page, PEOPLE);
      await openUsersTab(page);

      await (await openMenuFor(page, "Sam Okafor"))
        .getByRole("menuitem", { name: "Remove from service point…" })
        .click();
      const dialog = page.getByRole("dialog", { name: "Remove Sam Okafor from this service point?" });
      await expect(dialog).toBeVisible();

      await dialog.getByRole("button", { name: "Cancel" }).click();
      await expect(dialog).toBeHidden();
      expect(mock.calls).toHaveLength(0);
      await expect(rowFor(page, "Sam Okafor")).toBeVisible();

      await (await openMenuFor(page, "Sam Okafor"))
        .getByRole("menuitem", { name: "Remove from service point…" })
        .click();
      await page.getByRole("dialog").getByRole("button", { name: "Remove" }).click();

      await expect(page.getByText("Removed user from service point")).toBeVisible();
      expect(mock.callsTo("DELETE", "active-group")[0].body).toMatchObject({ userId: "sam" });
      expect(mock.callsTo("PUT", "leave")[0].body).toMatchObject({ userId: "sam" });
      await expect(rowFor(page, "Sam Okafor")).toHaveCount(0);
    }
  );

  test("does not scroll a list that fits on the screen", { tag: "@local" }, async ({ page }) => {
    // Tall enough for four people below the filters and header.
    await page.setViewportSize({ width: 1280, height: 1000 });
    await mockServicePointMembers(page, PEOPLE);
    const list = await openUsersTab(page);

    expect(await isScrollable(list.locator("xpath=.."))).toBe(false);
    expect(await isPanelAroundScrollable(list)).toBe(false);
  });

  test(
    "scrolls only the rows of a long list, keeping the filters and header in place",
    { tag: "@local" },
    async ({ page }) => {
      await mockServicePointMembers(page, manyPeople(40));
      const list = await openUsersTab(page);

      const scroller = list.locator("xpath=..");
      expect(await isScrollable(scroller)).toBe(true);
      // The rows scroll, not the panel around them: one scrollbar, not two.
      expect(await isPanelAroundScrollable(list)).toBe(false);

      const header = page.getByText("Person", { exact: true });
      const filters = filterButton(page, "All", 40);
      const headerBefore = await header.boundingBox();
      const filtersBefore = await filters.boundingBox();

      await scroller.evaluate((el) => el.scrollTo(0, el.scrollHeight));
      await expect(rowFor(page, "Person39 Example")).toBeInViewport();

      expect(await header.boundingBox()).toEqual(headerBefore);
      expect(await filters.boundingBox()).toEqual(filtersBefore);
      await expect(header).toBeInViewport();
      await expect(filters).toBeInViewport();
    }
  );

  test(
    "uses the same list for each service point on the operator page",
    { tag: "@local" },
    async ({ page }) => {
      await mockServicePointMembers(page, PEOPLE);
      await page.goto("/service-points");
      await expect(page.getByRole("grid")).toBeVisible({ timeout: 15000 });

      const lists = page.getByRole("list", { name: /people$/ });
      await expect(lists.first()).toBeVisible({ timeout: 15000 });
      await expect(page.getByRole("group", { name: "Show" }).first()).toBeVisible();
      await expect(lists.first()).not.toContainText("service-point-admin:");
      await expect(lists.first()).not.toContainText("Riley Operations");
    }
  );
});
