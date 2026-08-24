import "dotenv/config"
import { defineConfig, devices } from "@playwright/test";

const { BASE_URL } = process.env;

if (!BASE_URL || BASE_URL === "undefined") {
  console.error("All environment variables must be set.");
  process.exit(1);
}

/**
 * Read environment variables from file.
 * https://github.com/motdotla/dotenv
 */
// require('dotenv').config();

/**
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: "./e2e",
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 0,
  /* Opt out of parallel tests on CI. */
  workers: process.env.CI ? 1 : undefined,
  /* Reporter to use. See https://playwright.dev/docs/test-reporters */
  reporter: "html",
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    /* Base URL to use in actions like `await page.goto('/')`. */
    baseURL: BASE_URL,

    /* Collect trace when retrying the failed test. See https://playwright.dev/docs/trace-viewer */
    trace: "on-first-retry",
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: "setup",
      testMatch: /auth\/setup\.ts/,
    },
    {
      name: "setup-operator",
      testMatch: /auth\/setup-operator\.ts/,
    },
    {
      name: "setup-unapproved-admin",
      testMatch: /auth\/setup-unapproved-admin\.ts/,
    },

    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        storageState: "e2e/.auth/user.json",
      },
      dependencies: ["setup"],
      // Role-specific suites run under their own project below, against
      // their own storageState - excluded here so they don't also run
      // against the default service-point-user session.
      testIgnore: [/tests[\\/]operator[\\/]/, /tests[\\/]unapproved-admin[\\/]/],
    },

    // RAID-659 / RAID-480: tests that require the `operator` role to see the
    // "Manage service points" page.
    {
      name: "chromium-operator",
      use: {
        ...devices["Desktop Chrome"],
        storageState: "e2e/.auth/operator.json",
      },
      dependencies: ["setup-operator"],
      testMatch: /tests[\\/]operator[\\/]/,
    },

    // RAID-608: tests that require a flat group-admin with only a raw,
    // unapproved membership of the raid-au service point's group.
    {
      name: "chromium-unapproved-admin",
      use: {
        ...devices["Desktop Chrome"],
        storageState: "e2e/.auth/unapproved-admin.json",
      },
      dependencies: ["setup-unapproved-admin"],
      testMatch: /tests[\\/]unapproved-admin[\\/]/,
    },

    // {
    //   name: "firefox",
    //   use: { ...devices["Desktop Firefox"] },
    // },

    // {
    //   name: "webkit",
    //   use: { ...devices["Desktop Safari"] },
    // },

    /* Test against mobile viewports. */
    // {
    //   name: 'Mobile Chrome',
    //   use: { ...devices['Pixel 5'] },
    // },
    // {
    //   name: 'Mobile Safari',
    //   use: { ...devices['iPhone 12'] },
    // },

    /* Test against branded browsers. */
    // {
    //   name: 'Microsoft Edge',
    //   use: { ...devices['Desktop Edge'], channel: 'msedge' },
    // },
    // {
    //   name: 'Google Chrome',
    //   use: { ...devices['Desktop Chrome'], channel: 'chrome' },
    // },
  ],

  /* Run local dev server before starting tests (skip in CI where we test against a deployed environment) */
  ...(!process.env.CI && {
    webServer: {
      command: "npm run dev",
      url: BASE_URL,
      reuseExistingServer: true,
    },
  }),
});
