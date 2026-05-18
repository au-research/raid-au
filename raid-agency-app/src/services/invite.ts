import { authService } from "@/services/auth-service.ts";
import { API_CONSTANTS } from "@/constants/apiConstants";
import { getRuntimeConfig } from "@/config";

const SUB_DOMAIN = "invite";

// The invite service doesn't exist in dev, so dev maps to test.
const getInviteEnv = () => {
  const env = getRuntimeConfig().environment;
  return env === "dev" ? "test" : env;
};

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
    API_CONSTANTS.INVITE.SEND(SUB_DOMAIN, getInviteEnv()),
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
    API_CONSTANTS.INVITE.FETCH(SUB_DOMAIN, getInviteEnv()),
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
    API_CONSTANTS.INVITE.ACCEPT(SUB_DOMAIN, getInviteEnv()),
    {
      method: "POST",
      body: JSON.stringify({ code, handle }),
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
    API_CONSTANTS.INVITE.REJECT(SUB_DOMAIN, getInviteEnv()),
    {
      method: "POST",
      body: JSON.stringify({ code, handle }),
    }
  );
  return await response.json();
}
