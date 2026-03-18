// RAID-536: Page object for the external Keycloak login page

import { type Page } from "@playwright/test";

export class KeycloakLoginPage {
  constructor(private readonly page: Page) {}

  async login(username: string, password: string): Promise<void> {
    await this.page.locator("#username").fill(username);
    await this.page.locator("#password").fill(password);
    await this.page.locator('[type="submit"]').click();
  }
}
