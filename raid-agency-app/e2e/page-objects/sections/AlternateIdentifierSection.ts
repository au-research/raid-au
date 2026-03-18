// RAID-536: Section page object for the Alternate Identifiers form section
// Card id="alternateIdentifier", test-id="alternateIdentifier-form"
// Field names: alternateIdentifier.{index}.id, alternateIdentifier.{index}.type

import { type Page } from "@playwright/test";

export class AlternateIdentifierSection {
  private readonly card;

  constructor(private readonly page: Page) {
    this.card = page.locator("#alternateIdentifier");
  }

  async addItem(): Promise<void> {
    await this.card
      .getByRole("button", { name: "Add Alternate Identifier" })
      .click();
  }

  async fillId(index: number, value: string): Promise<void> {
    await this.page
      .locator(`#alternateIdentifier\\.${index}\\.id`)
      .fill(value);
  }

  async fillType(index: number, value: string): Promise<void> {
    await this.page
      .locator(`#alternateIdentifier\\.${index}\\.type`)
      .fill(value);
  }
}
