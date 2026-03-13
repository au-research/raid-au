/**
 * Keycloak Authentication Service
 *
 * This module provides functionality for interacting with the Keycloak authentication server.
 * It handles token refreshing and authentication-related operations.
 */
import { ApiTokenRequest, RequestTokenResponse } from "@/types";

/**
 * Keycloak configuration object containing connection details
 * Loaded from environment variables
 */
const KEYCLOAK_CONFIG = {
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
} as const;

/**
 * Fetches a new API token from Keycloak using the provided refresh token
 *
 * This function calls the Keycloak token endpoint to get a new access token
 * using the OAuth2 refresh token flow.
 *
 * @param refreshToken - The refresh token to use for getting a new access token
 * @returns Promise resolving to a token response containing access_token and refresh_token
 * @throws Error if Keycloak configuration is missing or if the token request fails
 */
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
