// RAID-608: Auth setup for the "unapproved admin" role — a flat group-admin
// who is only a raw, self-joined (never approved) member of the raid-au
// service point's Keycloak group. Used to verify that such a user cannot
// approve access requests for that service point (HELP-2844). Saves session
// state to e2e/.auth/unapproved-admin.json.

import { test as setup } from "@playwright/test";
import { fileURLToPath } from "url";
import path from "path";
import { authenticateAndSaveState } from "./authenticate";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const authFile = path.join(__dirname, "../.auth/unapproved-admin.json");

setup("authenticate as unapproved group-admin", async ({ page, baseURL }) => {
  const username = process.env.VITE_KEYCLOAK_E2E_UNAPPROVED_ADMIN_USER;
  const password = process.env.VITE_KEYCLOAK_E2E_UNAPPROVED_ADMIN_PASSWORD;
  const keycloakUrl = process.env.VITE_KEYCLOAK_URL;
  const realm = process.env.VITE_KEYCLOAK_REALM;
  const clientId = process.env.VITE_KEYCLOAK_CLIENT_ID;

  if (!username) {
    throw new Error(
      "VITE_KEYCLOAK_E2E_UNAPPROVED_ADMIN_USER environment variable is not set. " +
        "Add it to your .env file."
    );
  }
  if (!password) {
    throw new Error(
      "VITE_KEYCLOAK_E2E_UNAPPROVED_ADMIN_PASSWORD environment variable is not set. " +
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
