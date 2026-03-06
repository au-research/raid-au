// RAID-536: Page object for the RAiD create/edit form page

import { type Page, expect } from "@playwright/test";

export class RaidFormPage {
  constructor(private readonly page: Page) {}

  async goto(path: string): Promise<void> {
    await this.page.goto(path);
    await this.waitForFormReady();
  }

  async save(): Promise<void> {
    await this.page.getByTestId("save-raid-button").click();
  }

  async waitForFormReady(): Promise<void> {
    await expect(this.page.getByTestId("raid-form")).toBeVisible({
      timeout: 15000,
    });
  }

  async waitForSuccessfulSave(): Promise<void> {
    // After a successful save, the app navigates away from the /create or /edit URL
    // to the view page at /raids/{prefix}/{suffix}
    await this.page.waitForURL(/\/raids\/\d+\/\d+$/, { timeout: 15000 });
  }

  currentUrl(): string {
    return this.page.url();
  }
}
