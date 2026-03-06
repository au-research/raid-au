// RAID-536: Section page object for the Spatial Coverages form section
// Card id="spatialCoverage", test-id="spatialCoverage-form"
// Field names: spatialCoverage.{index}.id

import { type Page } from "@playwright/test";

export class SpatialCoverageSection {
  private readonly card;

  constructor(private readonly page: Page) {
    this.card = page.locator("#spatialCoverage");
  }

  async addItem(): Promise<void> {
    await this.card
      .getByRole("button", { name: "Add Spatial Coverage" })
      .click();
  }

  async fillId(index: number, value: string): Promise<void> {
    await this.page.locator(`#spatialCoverage\\.${index}\\.id`).fill(value);
  }

  async addPlace(coverageIndex: number): Promise<void> {
    await this.card.getByRole("button", { name: "Add Place" }).click();
  }

  async fillPlaceName(coverageIndex: number, placeIndex: number, value: string): Promise<void> {
    await this.page
      .locator(`#spatialCoverage\\.${coverageIndex}\\.place\\.${placeIndex}\\.text`)
      .fill(value);
  }
}
