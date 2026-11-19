import { AppConfig } from "./Appconfig";
import { RuntimeConfig } from "./RuntimeConfig";
import { defaultConfig } from "./DefaultConfig";
import { deepMerge } from "./deepMerge";
import {
  ConfigError,
  ConfigIssues,
  hasConfigIssues,
  validateConfig,
} from "./ConfigValidation";
import { RawBranding, sanitiseBranding } from "./SanitiseBranding";

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
  if (issues.retired && issues.retired.length > 0) {
    parts.push(`no longer used: ${issues.retired.join(", ")}`);
  }
  return parts.join("; ");
}

export async function loadConfig(): Promise<{
  runtime: RuntimeConfig;
  app: AppConfig;
  /**
   * The theme, header and footer sections from the top level of the file, with
   * unusable values removed. Pass this to resolveBranding() with the realm's
   * login theme (RAID-1065).
   */
  branding: RawBranding;
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
    // Optional. Anything that is not a non-blank text is ignored (and named in
    // the issues by validateConfig).
    contactEmail:
      typeof raw.contactEmail === "string" && raw.contactEmail.trim() !== ""
        ? raw.contactEmail
        : undefined,
    googleAnalytics: raw.googleAnalytics ?? {},
    services: raw.services ?? {},
    app: { ...raw.app, orcid: { ...raw.app?.orcid } },
  };

  // Until the header and footer read the resolved branding, the app still takes
  // its branding from the old "branding" wrapper. Phases 4 and 5 of RAID-1065
  // move the header and footer across and remove this.
  const legacyBranding = raw.branding ?? {};
  const app: AppConfig = deepMerge(defaultConfig, {
    default: legacyBranding.default,
    header: legacyBranding.header,
    footer: legacyBranding.footer,
    content: legacyBranding.content,
    theme: legacyBranding.theme,
  });

  return { runtime, app, branding: sanitiseBranding(raw), issues: warning };
}
