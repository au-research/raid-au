// RAID-536: Shared direct-grant authentication helper, extracted so multiple
// role-specific setup projects (RAID-659/480/608 need operator and group-admin
// sessions alongside the default service-point-user session) can reuse the
// same token-seeding logic against different credentials and storage files.

import { type Page } from "@playwright/test";

export async function authenticateAndSaveState({
  page,
  baseURL,
  username,
  password,
  keycloakUrl,
  realm,
  clientId,
  authFilePath,
}: {
  page: Page;
  baseURL: string | undefined;
  username: string;
  password: string;
  keycloakUrl: string;
  realm: string;
  clientId: string;
  authFilePath: string;
}): Promise<void> {
  const tokenResponse = await page.request.post(
    `${keycloakUrl}/realms/${realm}/protocol/openid-connect/token`,
    {
      form: {
        grant_type: "password",
        client_id: clientId,
        username,
        password,
      },
    }
  );

  if (!tokenResponse.ok()) {
    throw new Error(
      `Direct grant token request failed for ${username}: ${tokenResponse.status()} ${await tokenResponse.text()}`
    );
  }

  const tokens = await tokenResponse.json();

  // Visit the app origin so the seeded key lands in the right localStorage origin,
  // without ever hitting Keycloak's rendered login page.
  await page.goto(baseURL ?? "/");
  await page.evaluate(
    ({ token, refreshToken, idToken }) => {
      localStorage.setItem(
        "__e2e_auth_tokens__",
        JSON.stringify({ token, refreshToken, idToken })
      );
    },
    {
      token: tokens.access_token,
      refreshToken: tokens.refresh_token,
      idToken: tokens.id_token,
    }
  );

  // Reload so KeycloakContext picks up the seeded tokens on init.
  await page.reload();
  await page.waitForFunction(
    () => !window.location.pathname.startsWith("/login"),
    { timeout: 15000 }
  );
  await page.waitForLoadState("networkidle");

  await page.context().storageState({ path: authFilePath });
}
