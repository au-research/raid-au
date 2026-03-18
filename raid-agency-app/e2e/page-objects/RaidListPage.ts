// RAID-536: Page object for the /raids list page

import { type Page, type Locator, expect } from "@playwright/test";

export class RaidListPage {
  constructor(private readonly page: Page) {}

  async goto(): Promise<void> {
    await this.page.goto("/raids");
    await this.waitForListToLoad();
  }

  async findRow(titleText: string): Promise<Locator> {
    return this.page.getByRole("row").filter({ hasText: titleText });
  }

  async waitForListToLoad(): Promise<void> {
    // The DataGrid renders rows once data is loaded
    await expect(
      this.page.getByRole("grid").or(this.page.getByText("No RAiDs found"))
    ).toBeVisible({ timeout: 15000 });
  }
}
