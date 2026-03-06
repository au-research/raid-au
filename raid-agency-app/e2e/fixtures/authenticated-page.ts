// RAID-536: Custom fixture that provides an authenticated page using saved session state

import { test as base } from "@playwright/test";
import { fileURLToPath } from "url";
import path from "path";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const authFile = path.join(__dirname, "../.auth/user.json");

// Extend base test with storageState so all tests using this fixture
// start with an authenticated session without needing to log in each time
export const test = base.extend({
  storageState: authFile,
});

export { expect } from "@playwright/test";
