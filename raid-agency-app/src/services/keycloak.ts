/**
 * Keycloak Authentication Service
 *
 * This module provides functionality for interacting with the Keycloak authentication server.
 * It handles token refreshing and authentication-related operations.
 */
import { ApiTokenRequest, RequestTokenResponse } from "@/types";
import { getRuntimeConfig } from "@/config";

/**
 * Fetches a new API token from Keycloak using the provided refresh token
 *
 * @param refreshToken - The refresh token to use for getting a new access token
 * @returns Promise resolving to a token response containing access_token and refresh_token
 * @throws Error if Keycloak configuration is missing or if the token request fails
 */
export async function fetchApiTokenFromKeycloak({
  refreshToken,
}: ApiTokenRequest): Promise<RequestTokenResponse> {
  const { keycloak } = getRuntimeConfig();

  const tokenEndpoint = `${keycloak.url}/realms/${keycloak.realm}/protocol/openid-connect/token`;

  try {
    const response = await fetch(tokenEndpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        client_id: keycloak.clientId,
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
