// RAID-536: Section page object for the Related Objects form section
// Card id="relatedObject", test-id="relatedObject-form"
// Field names: relatedObject.{index}.id, relatedObject.{index}.type.id

import { type Page } from "@playwright/test";

export class RelatedObjectSection {
  private readonly card;

  constructor(private readonly page: Page) {
    this.card = page.locator("#relatedObject");
  }

  async addItem(): Promise<void> {
    await this.card
      .getByRole("button", { name: "Add Related Object" })
      .click();
  }

  async fillId(index: number, value: string): Promise<void> {
    await this.page.locator(`#relatedObject\\.${index}\\.id`).fill(value);
  }

  async selectType(index: number, value: string): Promise<void> {
    await this.page
      .locator(`#relatedObject\\.${index}\\.type\\.id`)
      .click();
    await this.page.getByRole("option", { name: value }).click();
  }
}
