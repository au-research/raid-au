import { AppConfig } from "./Appconfig";
import { RuntimeConfig } from "./RuntimeConfig";
import { defaultConfig } from "./DefaultConfig";
import {
  ConfigError,
  ConfigIssues,
  hasConfigIssues,
  validateConfig,
} from "./ConfigValidation";

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

async function fetchConfigFile(): Promise<Record<string, any>> {
  let response: Response;
  try {
    response = await fetch("/app-config.json", {
      headers: { "Cache-Control": "no-cache" },
    });
  } catch (cause) {
    throw new ConfigError(
      "load-failed",
      `[Config] Failed to load /app-config.json: ${String(cause)}`
    );
  }

  if (!response.ok) {
    throw new ConfigError(
      "load-failed",
      `[Config] Failed to load /app-config.json: ${response.status} ${response.statusText}`
    );
  }

  // A missing file behind CloudFront's SPA fallback comes back as index.html
  // with a 200, so a parse failure is treated the same as a failed load.
  let raw: unknown;
  try {
    raw = await response.json();
  } catch {
    throw new ConfigError("load-failed", "[Config] /app-config.json is not valid JSON");
  }

  if (raw === null || typeof raw !== "object" || Array.isArray(raw)) {
    throw new ConfigError("load-failed", "[Config] /app-config.json is not a JSON object");
  }

  return raw as Record<string, any>;
}

function describeIssues(issues: ConfigIssues): string {
  const parts: string[] = [];
  if (issues.missing.length > 0) parts.push(`missing: ${issues.missing.join(", ")}`);
  if (issues.invalid.length > 0) parts.push(`invalid: ${issues.invalid.join(", ")}`);
  return parts.join("; ");
}

export async function loadConfig(): Promise<{
  runtime: RuntimeConfig;
  app: AppConfig;
  issues: ConfigIssues;
}> {
  const raw = await fetchConfigFile();
  const { blocking, warning } = validateConfig(raw);

  if (hasConfigIssues(blocking)) {
    throw new ConfigError(
      "invalid-fields",
      `[Config] Required fields in app-config.json are ${describeIssues(blocking)}`,
      blocking
    );
  }

  if (hasConfigIssues(warning)) {
    console.warn(`[Config] Fields in app-config.json are ${describeIssues(warning)}`);
  }

  // Missing optional blocks become empty objects so consumers that read
  // services.* or app.orcid.* get undefined values instead of a crash.
  const runtime: RuntimeConfig = {
    keycloak: raw.keycloak,
    apiBaseUrl: raw.apiBaseUrl,
    environment: raw.environment,
    supportEmail: raw.supportEmail,
    googleAnalytics: raw.googleAnalytics ?? {},
    services: raw.services ?? {},
    app: { ...raw.app, orcid: { ...raw.app?.orcid } },
  };

  const branding = raw.branding ?? {};
  const app: AppConfig = deepMerge(defaultConfig, {
    default: branding.default,
    header: branding.header,
    footer: branding.footer,
    content: branding.content,
    theme: branding.theme,
  });

  return { runtime, app, issues: warning };
}
