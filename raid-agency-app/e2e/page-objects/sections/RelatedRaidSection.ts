// RAID-536: Section page object for the Related RAiDs form section
// Card id="relatedRaid", test-id="relatedRaid-form"
// Field names: relatedRaid.{index}.id, relatedRaid.{index}.type.id

import { type Page } from "@playwright/test";

export class RelatedRaidSection {
  private readonly card;

  constructor(private readonly page: Page) {
    this.card = page.locator("#relatedRaid");
  }

  async addItem(): Promise<void> {
    await this.card
      .getByRole("button", { name: "Add Related RAiD" })
      .click();
  }

  async fillId(index: number, value: string): Promise<void> {
    await this.page.locator(`#relatedRaid\\.${index}\\.id`).fill(value);
  }

  async selectType(index: number, value: string): Promise<void> {
    await this.page
      .locator(`#relatedRaid\\.${index}\\.type\\.id`)
      .click();
    await this.page.getByRole("option", { name: value }).click();
  }
}
