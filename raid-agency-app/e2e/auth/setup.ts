// RAID-536: Global auth setup - runs once before all tests to authenticate
// and save session state to e2e/.auth/user.json
//
// Authenticates via Keycloak's direct grant (Resource Owner Password Credentials)
// token endpoint instead of driving the rendered login page. This keeps e2e auth
// independent of whichever Keycloak login theme is active — the custom themes
// (raid-custom, ardc-branding) render IDP-only buttons with no username/password
// form, so UI automation of the login page only works against Keycloak's default
// theme. The obtained tokens are seeded into localStorage under a key that
// KeycloakContext.tsx checks for; the app then initialises from those tokens
// directly instead of running its normal check-sso flow.

import { test as setup } from "@playwright/test";
import { fileURLToPath } from "url";
import path from "path";
import { authenticateAndSaveState } from "./authenticate";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const authFile = path.join(__dirname, "../.auth/user.json");

setup("authenticate", async ({ page, baseURL }) => {
  const username = process.env.VITE_KEYCLOAK_E2E_USER;
  const password = process.env.VITE_KEYCLOAK_E2E_PASSWORD;
  const keycloakUrl = process.env.VITE_KEYCLOAK_URL;
  const realm = process.env.VITE_KEYCLOAK_REALM;
  const clientId = process.env.VITE_KEYCLOAK_CLIENT_ID;

  if (!username) {
    throw new Error(
      "VITE_KEYCLOAK_E2E_USER environment variable is not set. " +
        "Add it to your .env file."
    );
  }
  if (!password) {
    throw new Error(
      "VITE_KEYCLOAK_E2E_PASSWORD environment variable is not set. " +
        "Add it to your .env file."
    );
  }
  if (!keycloakUrl || !realm || !clientId) {
    throw new Error(
      "VITE_KEYCLOAK_URL, VITE_KEYCLOAK_REALM and VITE_KEYCLOAK_CLIENT_ID " +
        "must all be set. Add them to your .env file."
    );
  }

  await authenticateAndSaveState({
    page,
    baseURL,
    username,
    password,
    keycloakUrl,
    realm,
    clientId,
    authFilePath: authFile,
  });
});
