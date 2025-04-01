import { ApiTokenRequest, RequestTokenResponse } from "@/types";

const KEYCLOAK_CONFIG = {
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
} as const;

export async function fetchApiTokenFromKeycloak({
  refreshToken,
}: ApiTokenRequest): Promise<RequestTokenResponse> {
  // Validate environment variables
  if (
    !KEYCLOAK_CONFIG.url ||
    !KEYCLOAK_CONFIG.realm ||
    !KEYCLOAK_CONFIG.clientId
  ) {
    throw new Error("Missing required Keycloak configuration");
  }

  const tokenEndpoint = `${KEYCLOAK_CONFIG.url}/realms/${KEYCLOAK_CONFIG.realm}/protocol/openid-connect/token`;

  try {
    const response = await fetch(tokenEndpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        client_id: KEYCLOAK_CONFIG.clientId,
        refresh_token: refreshToken,
      }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    throw error instanceof Error
      ? error
      : new Error("Failed to fetch token from Keycloak");
  }
}

// This is used to fetch the list of raid users, based on the customAttribute `userRaids`
export async function fetchRaidUsers({
  handle,
  token,
}: {
  handle: string;
  token?: string;
}): Promise<{ id: string; firstName: string; lastName: string }[]> {
  // Validate environment variables
  if (
    !KEYCLOAK_CONFIG.url ||
    !KEYCLOAK_CONFIG.realm ||
    !KEYCLOAK_CONFIG.clientId
  ) {
    throw new Error("Missing required Keycloak configuration");
  }

  const url = `${KEYCLOAK_CONFIG.url}/realms/${KEYCLOAK_CONFIG.realm}/raid/<tbd>`;

  try {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        handle,
      }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    throw error instanceof Error
      ? error
      : new Error("Failed to fetch raid users");
  }
}

export async function promoteRaidUserToRaidAdmin({
  handle,
  userId,
  token,
}: {
  handle: string;
  userId: string;
  token?: string;
}): Promise<void> {
  // Validate environment variables
  if (
    !KEYCLOAK_CONFIG.url ||
    !KEYCLOAK_CONFIG.realm ||
    !KEYCLOAK_CONFIG.clientId
  ) {
    throw new Error("Missing required Keycloak configuration");
  }

  const url = `${KEYCLOAK_CONFIG.url}/realms/${KEYCLOAK_CONFIG.realm}/raid/<tbd>`;

  try {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({
        handle,
        userId,
      }),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    throw error instanceof Error
      ? error
      : new Error("Failed promote user to raid admin");
  }
}
