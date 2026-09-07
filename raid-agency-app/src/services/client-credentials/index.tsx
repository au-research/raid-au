import { authService } from "@/services/auth-service.ts";
import { getRuntimeConfig } from "@/config";

const kcClientCredentialBase = () => {
  const { keycloak } = getRuntimeConfig();
  return `${keycloak.url}/realms/${keycloak.realm}/client-credential`;
};

export interface ClientCredential {
  clientId: string;
  label: string;
  createdAt: string;
  lastRotatedAt: string | null;
  enabled: boolean;
}

export interface ClientCredentialSecret extends Omit<ClientCredential, "enabled"> {
  secret: string;
}

// The create endpoint's 409 (credential cap reached) needs to be surfaced to
// the user with its own actionable message, not folded into a generic
// failure - so it carries the HTTP status through, unlike the other calls.
export class ClientCredentialApiError extends Error {
  constructor(message: string, public status: number) {
    super(message);
    this.name = "ClientCredentialApiError";
  }
}

export async function fetchClientCredentials({
  groupId,
  token,
}: {
  groupId: string;
  token: string | undefined;
}): Promise<ClientCredential[]> {
  try {
    if (token === undefined) {
      throw new Error("Error: Keycloak token not set");
    }
    const response = await authService.fetchWithAuth(
      `${kcClientCredentialBase()}?groupId=${encodeURIComponent(groupId)}`
    );
    return await response.json();
  } catch (error) {
    const errorMessage = "Error: Client credentials could not be fetched";
    console.error(errorMessage);
    throw new Error(errorMessage);
  }
}

export async function createClientCredential({
  groupId,
  label,
  token,
}: {
  groupId: string;
  label: string;
  token: string | undefined;
}): Promise<ClientCredentialSecret> {
  if (token === undefined) {
    throw new Error("Error: Keycloak token not set");
  }
  const response = await authService.fetchWithAuth(kcClientCredentialBase(), {
    method: "POST",
    body: JSON.stringify({ groupId, label }),
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new ClientCredentialApiError(
      body?.error || "Error: Client credential could not be created",
      response.status
    );
  }
  return await response.json();
}

export async function getClientCredentialSecret({
  clientId,
  token,
}: {
  clientId: string;
  token: string | undefined;
}): Promise<ClientCredentialSecret> {
  try {
    if (token === undefined) {
      throw new Error("Error: Keycloak token not set");
    }
    const response = await authService.fetchWithAuth(
      `${kcClientCredentialBase()}/secret?clientId=${encodeURIComponent(clientId)}`
    );
    return await response.json();
  } catch (error) {
    const errorMessage = "Error: Client credential secret could not be retrieved";
    console.error(errorMessage);
    throw new Error(errorMessage);
  }
}

export async function rotateClientCredential({
  clientId,
  token,
}: {
  clientId: string;
  token: string | undefined;
}): Promise<ClientCredentialSecret> {
  try {
    if (token === undefined) {
      throw new Error("Error: Keycloak token not set");
    }
    const response = await authService.fetchWithAuth(`${kcClientCredentialBase()}/rotate`, {
      method: "POST",
      body: JSON.stringify({ clientId }),
    });
    return await response.json();
  } catch (error) {
    const errorMessage = "Error: Client credential could not be rotated";
    console.error(errorMessage);
    throw new Error(errorMessage);
  }
}

export async function revokeClientCredential({
  clientId,
  token,
}: {
  clientId: string;
  token: string | undefined;
}): Promise<void> {
  try {
    if (token === undefined) {
      throw new Error("Error: Keycloak token not set");
    }
    await authService.fetchWithAuth(
      `${kcClientCredentialBase()}?clientId=${encodeURIComponent(clientId)}`,
      { method: "DELETE" }
    );
  } catch (error) {
    const errorMessage = "Error: Client credential could not be revoked";
    console.error(errorMessage);
    throw new Error(errorMessage);
  }
}
