import { getEnv } from "@/utils/api-utils/api-utils";
import {authService} from "@/services/auth-service.ts";
import { API_CONSTANTS } from "@/constants/apiConstants";

let currentEnv = getEnv();
if (currentEnv === "dev") {
  currentEnv = "test";
}
const subDomain = "invite";

export async function sendInvite({
  email,
  handle,
  orcid,
  title,
  token,
}: {
  email?: string;
  handle: string;
  orcid?: string;
  title: string;
  token: string;
} & ({ email: string } | { orcid: string })) {
  const response = await authService.fetchWithAuth(
    API_CONSTANTS.INVITE.SEND(subDomain, currentEnv),
    {
      method: "POST",
      body: JSON.stringify({
        inviteeEmail: email || "",
        inviteeOrcid: orcid || "",
        title,
        handle,
      }),
    }
  );

  if (!response.ok) {
    throw new Error("Failed to send invite");
  }

  return await response.json();
}

export async function fetchInvites({ token }: { token: string }) {
  const response = await authService.fetchWithAuth(
      API_CONSTANTS.INVITE.FETCH(subDomain, currentEnv),
  );
  return await response.json();
}

export async function acceptInvite({
  code,
  token,
  handle,
}: {
  code: string;
  token: string;
  handle: string;
}) {
  const response = await authService.fetchWithAuth(
    API_CONSTANTS.INVITE.ACCEPT(subDomain, currentEnv),
    {
      method: "POST",
      body: JSON.stringify({
        code,
        handle,
      }),
    }
  );
  return await response.json();
}

export async function rejectInvite({
  code,
  token,
  handle,
}: {
  code: string;
  token: string;
  handle: string;
}) {
  const response = await authService.fetchWithAuth(
    API_CONSTANTS.INVITE.REJECT(subDomain, currentEnv),
    {
      method: "POST",
      body: JSON.stringify({
        code,
        handle,
      }),
    }
  );
  return await response.json();
}
