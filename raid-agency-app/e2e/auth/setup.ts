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

  const tokenResponse = await page.request.post(
    `${keycloakUrl}/realms/${realm}/protocol/openid-connect/token`,
    {
      form: {
        grant_type: "password",
        client_id: clientId,
        username,
        password,
      },
    }
  );

  if (!tokenResponse.ok()) {
    throw new Error(
      `Direct grant token request failed: ${tokenResponse.status()} ${await tokenResponse.text()}`
    );
  }

  const tokens = await tokenResponse.json();

  // Visit the app origin so the seeded key lands in the right localStorage origin,
  // without ever hitting Keycloak's rendered login page.
  await page.goto(baseURL ?? "/");
  await page.evaluate(
    ({ token, refreshToken, idToken }) => {
      localStorage.setItem(
        "__e2e_auth_tokens__",
        JSON.stringify({ token, refreshToken, idToken })
      );
    },
    {
      token: tokens.access_token,
      refreshToken: tokens.refresh_token,
      idToken: tokens.id_token,
    }
  );

  // Reload so KeycloakContext picks up the seeded tokens on init.
  await page.reload();
  await page.waitForFunction(
    () => !window.location.pathname.startsWith("/login"),
    { timeout: 15000 }
  );
  await page.waitForLoadState("networkidle");

  // Save authenticated session state
  await page.context().storageState({ path: authFile });
});
