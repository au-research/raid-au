// RAID-536: Section page object for the Access form section
// Card id="access"
// Field names: access.type.id, access.statement.text, access.statement.language.id, access.embargoExpiry
// Embargo fields only appear when access type includes "c_f1cf" (Embargoed Access)

import { type Page } from "@playwright/test";

export class AccessSection {
  private readonly card;

  constructor(private readonly page: Page) {
    this.card = page.locator("#access");
  }

  async selectAccessType(value: string): Promise<void> {
    // MUI Select: click the select element, then pick the menu item by visible text
    await this.page.locator("#access\\.type\\.id").click();
    await this.page.getByRole("option", { name: value }).click();
  }

  async fillStatementText(value: string): Promise<void> {
    await this.page.locator("#access\\.statement\\.text").fill(value);
  }

  async selectStatementLanguage(value: string): Promise<void> {
    await this.page.locator("#access\\.statement\\.language\\.id").click();
    await this.page.getByRole("option", { name: value }).click();
  }

  async fillEmbargoExpiry(value: string): Promise<void> {
    await this.page.locator("#access\\.embargoExpiry").fill(value);
  }

  async isEmbargoFieldsVisible(): Promise<boolean> {
    return this.page.locator("#access\\.embargoExpiry").isVisible();
  }
}
