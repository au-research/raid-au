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

  /**
   * Pastes `value` into the id field via a genuine clipboard paste (not
   * `.fill()`), so the test exercises the same event path a user pasting a
   * URL would trigger. Caller must grant clipboard-read/write permissions
   * on the browser context first.
   */
  async pasteId(index: number, value: string): Promise<void> {
    const field = this.page.locator(`#relatedObject\\.${index}\\.id`);
    await field.click();
    await this.page.evaluate((text) => navigator.clipboard.writeText(text), value);
    await field.press("ControlOrMeta+V");
  }

  async selectType(index: number, value: string): Promise<void> {
    await this.page
      .locator(`#relatedObject\\.${index}\\.type\\.id`)
      .click();
    await this.page.getByRole("option", { name: value }).click();
  }

  async selectCategory(objectIndex: number, categoryIndex: number, value: string): Promise<void> {
    await this.page
      .locator(`#relatedObject\\.${objectIndex}\\.category\\.${categoryIndex}\\.id`)
      .click();
    await this.page.getByRole("option", { name: value }).click();
  }

  // ---- Bulk upload (RAID-801) ----

  async openBulkUpload(): Promise<void> {
    await this.card
      .getByRole("button", { name: "Upload Bulk Related Objects" })
      .click();
  }

  async uploadBulkFile(filePath: string): Promise<void> {
    await this.card.locator('input[type="file"]').setInputFiles(filePath);
  }

  /** The preview-table row whose Identifier cell holds this exact value. */
  bulkPreviewRow(identifier: string) {
    return this.card
      .locator("table tbody tr")
      .filter({ has: this.page.locator(`input[value="${identifier}"]`) });
  }
}
