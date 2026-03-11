// RAID-536: Section page object for the Descriptions form section
// Card id="description", test-id="description-form"
// Field names: description.{index}.text, description.{index}.type.id, description.{index}.language.id

import { type Page } from "@playwright/test";

export class DescriptionSection {
  private readonly card;

  constructor(private readonly page: Page) {
    this.card = page.locator("#description");
  }

  async addItem(): Promise<void> {
    await this.card.getByRole("button", { name: "Add Description" }).click();
  }

  async fillText(index: number, value: string): Promise<void> {
    await this.page.locator(`#description\\.${index}\\.text`).fill(value);
  }

  async selectType(index: number, value: string): Promise<void> {
    await this.page.locator(`#description\\.${index}\\.type\\.id`).click();
    await this.page.getByRole("option", { name: value }).click();
  }

  /**
   * Select a language via the Autocomplete widget (LanguageSelector).
   * Type a search term (e.g. "eng") and pick the matching option.
   */
  async selectLanguage(index: number, searchTerm: string, optionPattern: RegExp): Promise<void> {
    const input = this.card.getByLabel("Language");
    await input.fill(searchTerm);
    await this.page.getByRole("option", { name: optionPattern }).click();
  }
}
