import { API_CONSTANTS } from "@/constants/apiConstants";
import { authService } from "@/services/auth-service.ts";

// Review feedback (robleney-ardc, #701): the backend endpoint this calls now
// requires authentication - the agency app always has a token by the time it
// shows a contributor, so this follows the same no-token-parameter
// convention as raid-service.ts (authService.fetchWithAuth reaches into the
// Keycloak singleton itself).
export const fetchIsniName = async ({
  isni,
}: {
  isni: string;
}): Promise<string> => {
  const response = await authService.fetchWithAuth(API_CONSTANTS.ISNI.NAME(isni));

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const data = await response.json();
  return data.name ?? "";
};
