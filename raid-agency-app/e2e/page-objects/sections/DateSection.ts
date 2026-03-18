// RAID-536: Section page object for the Date form section
// Card id="date"
// Field names: date.startDate, date.endDate

import { type Page } from "@playwright/test";

export class DateSection {
  constructor(private readonly page: Page) {}

  async fillStartDate(value: string): Promise<void> {
    await this.page.locator("#date\\.startDate").fill(value);
  }

  async fillEndDate(value: string): Promise<void> {
    await this.page.locator("#date\\.endDate").fill(value);
  }
}
