import { authService } from "@/services/auth-service.ts";
import { API_CONSTANTS } from "@/constants/apiConstants";
import { getRuntimeConfig } from "@/config";

const kcGroupBase = () => {
  const { keycloak } = getRuntimeConfig();
  return `${keycloak.url}/realms/${keycloak.realm}/group`;
};

const kcLocalizationBase = () => {
  const { keycloak } = getRuntimeConfig();
  return `${keycloak.url}/realms/${keycloak.realm}/localization`;
};

export async function joinKeycloakGroup({
  token,
  groupId,
}: {
  token: string | undefined;
  groupId: string;
}) {
  try {
    if (token === undefined) {
      throw new Error("Error: Keycloak token not set");
    }
    const response = await authService.fetchWithAuth(`${kcGroupBase()}/join`, {
      method: "PUT",
      body: JSON.stringify({ groupId }),
    });
    return await response.json();
  } catch (error) {
    const errorMessage = "Error: Keycloak group could not be joined";
    console.error(errorMessage);
    throw new Error(errorMessage);
  }
}

export async function fetchAllKeycloakGroups({
  token,
}: {
  token: string | undefined;
}) {
  try {
    if (token === undefined) {
      throw new Error("Error: Keycloak token not set");
    }
    const response = await authService.fetchWithAuth(`${kcGroupBase()}/all`);
    return await response.json();
  } catch (error) {
    const errorMessage = "Error: Keycloak groups could not be fetched";
    console.error(errorMessage);
    throw new Error(errorMessage);
  }
}

export async function fetchCurrentUserKeycloakGroups({
  token,
}: {
  token: string | undefined;
}) {
  try {
    if (token === undefined) {
      throw new Error("Error: Keycloak token not set");
    }
    const response = await authService.fetchWithAuth(`${kcGroupBase()}/user-groups`);
    return await response.json();
  } catch (error) {
    const errorMessage = "Error: Keycloak groups could not be fetched";
    console.error(errorMessage);
    throw new Error(errorMessage);
  }
}

export async function setKeycloakUserAttribute({
  groupId,
  token,
}: {
  groupId: string;
  token: string | undefined;
}) {
  try {
    if (token === undefined) {
      throw new Error("Error: Keycloak token not set");
    }
    await authService.fetchWithAuth(`${kcGroupBase()}/active-group`, {
      method: "PUT",
      body: JSON.stringify({ activeGroupId: groupId }),
    });
  } catch (error) {
    const errorMessage = "Error: Keycloak group could not be joined";
    console.error(errorMessage);
    throw new Error(errorMessage);
  }
}

export async function fetchCurrentUserRor({
  token,
  tokenParsed,
}: {
  token: string | undefined;
  tokenParsed: Record<string, string> | undefined;
}): Promise<string | null> {
  try {
    if (tokenParsed === undefined) {
      throw new Error("Error: Keycloak token not set");
    }

    const response = await authService.fetchWithAuth(API_CONSTANTS.SERVICE_POINT.ALL);
    const servicePointGroupId = tokenParsed.service_point_group_id;
    const data = await response.json();

    let ror = null;
    for (const servicePoint of data) {
      if (servicePoint.groupId && servicePoint.groupId === servicePointGroupId) {
        ror = servicePoint.identifierOwner;
      }
    }
    return ror;
  } catch (error) {
    const errorMessage = "Error: Curren user ROR could not be determined";
    console.error(errorMessage);
    throw new Error(errorMessage);
  }
}

export const fetchKeycloakLocalization = async ({
  token,
  key,
  locale = "en",
}: {
  token: string | undefined;
  key: string;
  locale?: string;
}) => {
  const response = await fetch(
    `${kcLocalizationBase()}?key=${encodeURIComponent(key)}&locale=${encodeURIComponent(locale)}`,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );
  if (!response.ok) throw new Error("Failed to fetch localization");
  return response.json();
};
