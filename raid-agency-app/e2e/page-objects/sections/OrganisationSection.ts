// RAID-536: Section page object for the Organisations form section
// Card id="organisation", test-id="organisation-form"
// Organisation uses a ROR search widget (CustomizedInputBase) rather than a plain text input.
// The widget renders a search input that sets organisation.{index}.id on selection.

import { type Page } from "@playwright/test";

export class OrganisationSection {
  private readonly card;

  constructor(private readonly page: Page) {
    this.card = page.locator("#organisation");
  }

  async addItem(): Promise<void> {
    await this.card.getByRole("button", { name: "Add Organisation" }).click();
  }

  /**
   * Type a search term into the ROR search widget for the given index,
   * wait for suggestions, then click the first matching result.
   * The widget sets organisation.{index}.id on selection.
   */
  async searchAndSelect(index: number, searchTerm: string): Promise<void> {
    // The ROR widget renders an input within the organisation card at the given index
    const orgCard = this.card.getByTestId("organisation-form").nth(index);
    const searchInput = orgCard.locator('input[type="text"]').first();
    await searchInput.fill(searchTerm);
    // Wait for autocomplete options to appear and click the first one
    const firstOption = this.page.getByRole("option").first();
    await firstOption.waitFor({ timeout: 10000 });
    await firstOption.click();
  }
}
