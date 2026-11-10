const LOCAL_WEB_APP_URL = "http://localhost:7080";
const ARDC_ENVIRONMENTS = ["prod", "demo", "stage", "test"];

export interface AppUrlInput {
  appUrl?: string;
  raidEnv?: string;
}

/**
 * Resolves the base URL of the RAiD web app used by the Show, Edit and New
 * links. A configured `appUrl` always wins. `appUrl` is required by the data
 * fetch script, so the ARDC environment mapping and the localhost default only
 * apply where that check is bypassed, such as unit tests and CI stubs.
 */
export function resolveAppUrl({ appUrl, raidEnv }: AppUrlInput): string {
  const configured = appUrl?.trim();
  if (configured) {
    return configured.replace(/\/+$/, "");
  }

  if (raidEnv && ARDC_ENVIRONMENTS.includes(raidEnv)) {
    return `https://app.${raidEnv}.raid.org.au`;
  }

  return LOCAL_WEB_APP_URL;
}
