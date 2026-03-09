// RAID-536: Section page object for the Subjects form section
// Card id="subject"
// The subject section uses a tree-view widget (CodesContext) rather than simple inputs.
// Subjects are selected via a search box that filters the tree and checkboxes that toggle selection.

import { type Page } from "@playwright/test";

export class SubjectSection {
  private readonly card;

  constructor(private readonly page: Page) {
    this.card = page.locator("#subject");
  }

  /**
   * Search for a subject by text and select the first matching result in the tree view.
   * The subject tree widget sets the subject array in React Hook Form state.
   */
  async searchAndSelectFirst(searchTerm: string): Promise<void> {
    // The subject section has a search input for filtering the tree
    const searchInput = this.card.locator('input[type="text"]').first();
    await searchInput.fill(searchTerm);
    // Wait for filtered results and click the first checkbox
    const firstCheckbox = this.card
      .getByRole("checkbox")
      .first();
    await firstCheckbox.waitFor({ timeout: 10000 });
    await firstCheckbox.check();
  }

  async clearSearch(): Promise<void> {
    const searchInput = this.card.locator('input[type="text"]').first();
    await searchInput.clear();
  }
}
