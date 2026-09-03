// RAID-659 / RAID-480: Auth setup for the "operator" role, used by e2e tests
// that exercise the "Manage service points" page (ServicePointsOperatorView),
// which only renders for users holding the `operator` realm role. Saves
// session state to e2e/.auth/operator.json, separate from the default
// service-point-user session in e2e/.auth/user.json.

import { test as setup } from "@playwright/test";
import { fileURLToPath } from "url";
import path from "path";
import { authenticateAndSaveState } from "./authenticate";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const authFile = path.join(__dirname, "../.auth/operator.json");

setup("authenticate as operator", async ({ page, baseURL }) => {
  const username = process.env.VITE_KEYCLOAK_E2E_OPERATOR_USER;
  const password = process.env.VITE_KEYCLOAK_E2E_OPERATOR_PASSWORD;
  const keycloakUrl = process.env.VITE_KEYCLOAK_URL;
  const realm = process.env.VITE_KEYCLOAK_REALM;
  const clientId = process.env.VITE_KEYCLOAK_CLIENT_ID;

  if (!username) {
    throw new Error(
      "VITE_KEYCLOAK_E2E_OPERATOR_USER environment variable is not set. " +
        "Add it to your .env file."
    );
  }
  if (!password) {
    throw new Error(
      "VITE_KEYCLOAK_E2E_OPERATOR_PASSWORD environment variable is not set. " +
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
