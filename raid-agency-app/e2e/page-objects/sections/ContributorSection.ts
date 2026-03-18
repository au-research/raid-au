// RAID-536: Section page object for the Contributors form section
// Card id="contributor", test-id="contributor-form"
// Field names: contributor.{index}.id (ORCID), contributor.{index}.leader, contributor.{index}.contact

import { type Page } from "@playwright/test";

export class ContributorSection {
  private readonly card;

  constructor(private readonly page: Page) {
    this.card = page.locator("#contributor");
  }

  async addItem(): Promise<void> {
    await this.card.getByRole("button", { name: "Add Contributor" }).click();
  }

  async fillOrcidId(index: number, value: string): Promise<void> {
    await this.page.locator(`#contributor\\.${index}\\.id`).fill(value);
  }

  async checkLeader(index: number): Promise<void> {
    const checkbox = this.page.locator(`#contributor\\.${index}\\.leader`);
    if (!(await checkbox.isChecked())) {
      await checkbox.check();
    }
  }

  async checkContact(index: number): Promise<void> {
    const checkbox = this.page.locator(`#contributor\\.${index}\\.contact`);
    if (!(await checkbox.isChecked())) {
      await checkbox.check();
    }
  }
}
