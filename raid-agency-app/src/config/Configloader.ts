import { AppConfig } from "./Appconfig";
import { RuntimeConfig } from "./RuntimeConfig";
import { defaultConfig } from "./DefaultConfig";

function deepMerge<T extends Record<string, any>>(
  target: T,
  source: Partial<T>
): T {
  const result = { ...target };

  for (const key of Object.keys(source) as Array<keyof T>) {
    const sourceVal = source[key];
    const targetVal = target[key];

    if (
      sourceVal !== null &&
      sourceVal !== undefined &&
      typeof sourceVal === "object" &&
      !Array.isArray(sourceVal) &&
      typeof targetVal === "object" &&
      !Array.isArray(targetVal)
    ) {
      result[key] = deepMerge(
        targetVal as Record<string, any>,
        sourceVal as Record<string, any>
      ) as T[keyof T];
    } else if (sourceVal !== undefined) {
      result[key] = sourceVal as T[keyof T];
    }
  }

  return result;
}

function validateRuntimeFields(raw: Record<string, any>): void {
  const missing: string[] = [];

  if (!raw.keycloak?.url) missing.push("keycloak.url");
  if (!raw.keycloak?.realm) missing.push("keycloak.realm");
  if (!raw.keycloak?.clientId) missing.push("keycloak.clientId");
  if (!raw.apiBaseUrl) missing.push("apiBaseUrl");
  if (!raw.environment) missing.push("environment");
  if (!raw.supportEmail) missing.push("supportEmail");
  if (!raw.services?.orcid) missing.push("services.orcid");
  if (!raw.services?.staticProd) missing.push("services.staticProd");
  if (!raw.services?.staticBase) missing.push("services.staticBase");

  if (missing.length > 0) {
    throw new Error(
      `[Config] Missing required fields in app-config.json: ${missing.join(", ")}`
    );
  }
}

export async function loadConfig(): Promise<{
  runtime: RuntimeConfig;
  app: AppConfig;
}> {
  const response = await fetch("/app-config.json", {
    headers: { "Cache-Control": "no-cache" },
  });

  if (!response.ok) {
    throw new Error(
      `[Config] Failed to load /app-config.json: ${response.status} ${response.statusText}`
    );
  }

  const raw = await response.json();
  validateRuntimeFields(raw);

  const runtime: RuntimeConfig = {
    keycloak: raw.keycloak,
    apiBaseUrl: raw.apiBaseUrl,
    environment: raw.environment,
    supportEmail: raw.supportEmail,
    googleAnalytics: raw.googleAnalytics ?? {},
    services: raw.services,
    app: raw.app ?? {},
  };

  const branding = raw.branding ?? {};
  const app: AppConfig = deepMerge(defaultConfig, {
    default: branding.default,
    header: branding.header,
    footer: branding.footer,
    content: branding.content,
    theme: branding.theme,
  });

  return { runtime, app };
}
