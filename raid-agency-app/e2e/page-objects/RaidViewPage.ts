// RAID-536: Page object for the /raids/{prefix}/{suffix} view page

import { type Page, expect } from "@playwright/test";

export class RaidViewPage {
  constructor(private readonly page: Page) {}

  async goto(prefix: string, suffix: string): Promise<void> {
    await this.page.goto(`/raids/${prefix}/${suffix}`);
    await expect(this.page.locator("main")).toBeVisible({ timeout: 15000 });
  }

  async getPrimaryTitle(): Promise<string> {
    return this.page.locator("h1, h2").first().innerText();
  }

  currentUrl(): string {
    return this.page.url();
  }
}
