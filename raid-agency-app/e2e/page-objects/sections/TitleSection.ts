// RAID-536: Section page object for the Titles form section
// Card id="title", test-id="title-form"
// Field names: title.{index}.text, title.{index}.type.id, title.{index}.startDate,
//              title.{index}.endDate, title.{index}.language.id

import { type Page } from "@playwright/test";

export class TitleSection {
  private readonly card;

  constructor(private readonly page: Page) {
    this.card = page.locator("#title");
  }

  async addItem(): Promise<void> {
    await this.card.getByRole("button", { name: "Add Title" }).click();
  }

  async fillText(index: number, value: string): Promise<void> {
    await this.page.locator(`#title\\.${index}\\.text`).fill(value);
  }

  async fillStartDate(index: number, value: string): Promise<void> {
    await this.page.locator(`#title\\.${index}\\.startDate`).fill(value);
  }

  async fillEndDate(index: number, value: string): Promise<void> {
    await this.page.locator(`#title\\.${index}\\.endDate`).fill(value);
  }

  async selectType(index: number, value: string): Promise<void> {
    // MUI Select: click the select element, then pick the menu item
    await this.page.locator(`#title\\.${index}\\.type\\.id`).click();
    await this.page.getByRole("option", { name: value }).click();
  }

  async selectLanguage(index: number, value: string): Promise<void> {
    await this.page.locator(`#title\\.${index}\\.language\\.id`).click();
    await this.page.getByRole("option", { name: value }).click();
  }
}
