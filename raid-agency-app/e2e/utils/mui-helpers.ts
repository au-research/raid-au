// RAID-541: Shared MUI interaction helpers for e2e tests

import { type Page, expect } from "@playwright/test";

/**
 * Open a MUI Select by clicking it, collect all visible option labels,
 * assert that every expected label is present, then close with Escape.
 */
export async function assertSelectOptions(
  page: Page,
  selectId: string,
  expectedLabels: string[]
): Promise<void> {
  await page.locator(`#${selectId}`).click();

  // MUI renders the listbox as role="listbox"; options are role="option"
  const listbox = page.getByRole("listbox");
  await listbox.waitFor({ timeout: 5000 });

  const options = listbox.getByRole("option");
  const actualLabels = await options.allTextContents();

  for (const expected of expectedLabels) {
    expect(
      actualLabels.some((actual) => actual.trim() === expected.trim()),
      `Expected option "${expected.trim()}" in select #${selectId}. Actual options: ${JSON.stringify(actualLabels.map((l) => l.trim()))}`
    ).toBe(true);
  }

  await page.keyboard.press("Escape");
  // Wait for the listbox to close before proceeding
  await listbox.waitFor({ state: "hidden", timeout: 3000 });
}
