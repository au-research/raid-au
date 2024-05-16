import { expect, test } from "@playwright/test";
import "dotenv/config";
import login from "./utils/login";

const BASE_URL = "http://localhost:7080/";

const SELECTORS = {
  titleField: '[data-testid="title-input"]',
  addTitleButton: '[aria-label="Add Title"]',
  raidNavLink: '[data-testid="raid-navlink"]',
  editRaidButton: '[data-testid="edit-raid-button"]',
  saveRaidButton: '[data-testid="save-raid-button"]',
};

test.beforeEach(async ({ page }) => {
  await login(page);
});

test.describe("Update RAiD - Titles component", () => {
  test("button to update raid should be disabled when title is empty", async ({
    page,
  }) => {
    await page.goto(BASE_URL);

    await page.waitForSelector(SELECTORS.raidNavLink);
    await page.locator(SELECTORS.raidNavLink).first().click();

    await page.waitForSelector(SELECTORS.editRaidButton);
    await page.locator(SELECTORS.editRaidButton).first().click();

    const firstTitleField = page.locator(SELECTORS.titleField).first();
    await firstTitleField.fill("");

    const saveRaidButton = page.locator(SELECTORS.saveRaidButton).first();
    expect(await saveRaidButton.isDisabled()).toBe(true);
  });
});
