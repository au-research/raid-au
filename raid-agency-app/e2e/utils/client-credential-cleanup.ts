// RAID-922: Teardown for e2e tests that create service point client
// credentials. Revoking through the UI only disables the Keycloak client and
// depends on the page having rendered the credential, so every run used to
// leave a client behind. This deletes the client through the credential SPI's
// delete endpoint (RAID-921) instead, independent of UI state.

import { type APIRequestContext } from "@playwright/test";

export interface CreatedCredential {
  groupId?: string;
  clientId?: string;
}

interface CredentialSummary {
  clientId: string;
  label: string;
}

function keycloakConfig() {
  const keycloakUrl = process.env.VITE_KEYCLOAK_URL;
  const realm = process.env.VITE_KEYCLOAK_REALM;
  const clientId = process.env.VITE_KEYCLOAK_CLIENT_ID;
  const username = process.env.VITE_KEYCLOAK_E2E_OPERATOR_USER;
  const password = process.env.VITE_KEYCLOAK_E2E_OPERATOR_PASSWORD;
  if (!keycloakUrl || !realm || !clientId || !username || !password) {
    throw new Error(
      "VITE_KEYCLOAK_URL, VITE_KEYCLOAK_REALM, VITE_KEYCLOAK_CLIENT_ID, " +
        "VITE_KEYCLOAK_E2E_OPERATOR_USER and VITE_KEYCLOAK_E2E_OPERATOR_PASSWORD " +
        "must all be set to clean up client credentials."
    );
  }
  return { keycloakUrl, realm, clientId, username, password };
}

// A fresh token rather than the one in the saved storage state, which may
// have expired by the time teardown runs.
async function operatorToken(request: APIRequestContext): Promise<string> {
  const { keycloakUrl, realm, clientId, username, password } = keycloakConfig();
  const response = await request.post(
    `${keycloakUrl}/realms/${realm}/protocol/openid-connect/token`,
    { form: { grant_type: "password", client_id: clientId, username, password } }
  );
  if (!response.ok()) {
    throw new Error(
      `Operator token request failed: ${response.status()} ${await response.text()}`
    );
  }
  return (await response.json()).access_token;
}

function credentialBase(): string {
  const { keycloakUrl, realm } = keycloakConfig();
  return `${keycloakUrl}/realms/${realm}/client-credential`;
}

async function findClientIdByLabel(
  request: APIRequestContext,
  token: string,
  groupId: string,
  label: string
): Promise<string | undefined> {
  const response = await request.get(
    `${credentialBase()}?groupId=${encodeURIComponent(groupId)}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  if (!response.ok()) {
    throw new Error(
      `Listing credentials for group ${groupId} failed: ${response.status()} ${await response.text()}`
    );
  }
  const credentials: CredentialSummary[] = await response.json();
  return credentials.find((credential) => credential.label === label)?.clientId;
}

/**
 * Permanently deletes the credential a test created. Uses the client ID
 * captured from the create response; if that was never seen (for example the
 * response timed out on the browser side while Keycloak still created the
 * client), falls back to finding the credential by its unique label within
 * the group. Does nothing if the create request was never sent.
 */
export async function deleteCreatedCredential(
  request: APIRequestContext,
  created: CreatedCredential,
  label: string
): Promise<void> {
  if (!created.clientId && !created.groupId) {
    return;
  }

  const token = await operatorToken(request);
  const clientId =
    created.clientId ??
    (await findClientIdByLabel(request, token, created.groupId!, label));
  if (!clientId) {
    return;
  }

  const response = await request.delete(
    `${credentialBase()}/delete?clientId=${encodeURIComponent(clientId)}`,
    { headers: { Authorization: `Bearer ${token}` } }
  );
  // 404 means it is already gone, which is the outcome teardown wants.
  if (response.status() !== 204 && response.status() !== 404) {
    throw new Error(
      `Deleting client credential ${clientId} failed: ${response.status()} ${await response.text()}`
    );
  }
}
