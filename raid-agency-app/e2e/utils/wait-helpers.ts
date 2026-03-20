// RAID-536: Shared wait helpers for e2e tests

import { type Page, expect } from "@playwright/test";

/**
 * Wait for a MUI Snackbar to appear containing the given text.
 * Snackbars are used for success/error feedback after save operations.
 */
export async function waitForSnackbar(
  page: Page,
  text: string,
  timeout = 10000
): Promise<void> {
  await expect(page.getByRole("alert").filter({ hasText: text })).toBeVisible({
    timeout,
  });
}

/**
 * Wait for the RAiD form to be visible and interactive.
 */
export async function waitForFormReady(page: Page): Promise<void> {
  await expect(page.getByTestId("raid-form")).toBeVisible({ timeout: 15000 });
}

/**
 * Extract the RAiD handle ({prefix}/{suffix}) from the current page URL.
 * Expects a URL of the form /raids/{prefix}/{suffix}.
 * Returns the handle string, e.g. "10378.1/123456".
 */
export function extractHandleFromUrl(url: string): string {
  const match = url.match(/\/raids\/(\d+\.\d+\/\d+)$/);
  if (!match) {
    throw new Error(
      `Could not extract RAiD handle from URL: ${url}. ` +
        `Expected URL pattern /raids/{prefix}/{suffix}.`
    );
  }
  return match[1];
}

/**
 * Generate an embargo expiry date N months in the future (YYYY-MM-DD).
 * The API enforces a maximum of 18 months, so default to 12 months.
 */
export function futureDate(monthsAhead = 12): string {
  const d = new Date();
  d.setMonth(d.getMonth() + monthsAhead);
  return d.toISOString().split("T")[0];
}

/**
 * Extract prefix and suffix separately from the current page URL.
 * Returns [prefix, suffix], e.g. ["10378.1", "123456"].
 */
export function extractPrefixSuffixFromUrl(url: string): [string, string] {
  const match = url.match(/\/raids\/([^/]+)\/([^/]+)$/);
  if (!match) {
    throw new Error(
      `Could not extract prefix/suffix from URL: ${url}. ` +
        `Expected URL pattern /raids/{prefix}/{suffix}.`
    );
  }
  return [match[1], match[2]];
}
