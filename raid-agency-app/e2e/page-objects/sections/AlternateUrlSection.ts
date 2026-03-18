// RAID-536: Section page object for the Alternate URLs form section
// Card id="alternateUrl", test-id="alternateUrl-form"
// Field names: alternateUrl.{index}.url

import { type Page } from "@playwright/test";

export class AlternateUrlSection {
  private readonly card;

  constructor(private readonly page: Page) {
    this.card = page.locator("#alternateUrl");
  }

  async addItem(): Promise<void> {
    await this.card
      .getByRole("button", { name: "Add Alternate Url" })
      .click();
  }

  async fillUrl(index: number, value: string): Promise<void> {
    await this.page.locator(`#alternateUrl\\.${index}\\.url`).fill(value);
  }
}
