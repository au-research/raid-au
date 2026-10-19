// Validation rules for app-config.json (RAID-941).
//
// "blocking" fields are ones the app cannot start without (no login, no API).
// "warning" fields degrade a feature when missing, so the app still starts and
// shows a banner under the header naming them.
//
// Issues only ever carry field paths, never the values found in the file.

export type ConfigIssueLevel = "blocking" | "warning";

type FieldCheck = "text" | "url";

interface FieldRule {
  path: string;
  level: ConfigIssueLevel;
  check: FieldCheck;
}

export const CONFIG_FIELD_RULES: readonly FieldRule[] = [
  { path: "keycloak.url", level: "blocking", check: "url" },
  { path: "keycloak.realm", level: "blocking", check: "text" },
  { path: "keycloak.clientId", level: "blocking", check: "text" },
  { path: "apiBaseUrl", level: "blocking", check: "url" },
  { path: "environment", level: "blocking", check: "text" },
  { path: "supportEmail", level: "warning", check: "text" },
  { path: "services.orcid", level: "warning", check: "url" },
  { path: "services.staticProd", level: "warning", check: "url" },
  { path: "services.staticBase", level: "warning", check: "url" },
  { path: "app.orcid.placeholder", level: "warning", check: "text" },
  { path: "app.orcid.helpText", level: "warning", check: "text" },
];

export interface ConfigIssues {
  missing: string[];
  invalid: string[];
}

export const NO_CONFIG_ISSUES: ConfigIssues = { missing: [], invalid: [] };

export function hasConfigIssues(issues: ConfigIssues): boolean {
  return issues.missing.length > 0 || issues.invalid.length > 0;
}

export function isUrlField(path: string): boolean {
  return CONFIG_FIELD_RULES.some((rule) => rule.path === path && rule.check === "url");
}

function readPath(raw: Record<string, unknown>, path: string): unknown {
  return path.split(".").reduce<unknown>((node, key) => {
    if (node === null || typeof node !== "object") return undefined;
    return (node as Record<string, unknown>)[key];
  }, raw);
}

function isHttpUrl(value: string): boolean {
  // services.staticBase holds a literal "{env}" placeholder that the app
  // fills in later, so substitute it before parsing.
  try {
    const url = new URL(value.replace("{env}", "env"));
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

type FieldState = "ok" | "missing" | "invalid";

function checkField(value: unknown, check: FieldCheck): FieldState {
  if (value === undefined || value === null) return "missing";
  if (typeof value !== "string") return "invalid";
  if (value.trim() === "") return "missing";
  if (check === "url" && !isHttpUrl(value)) return "invalid";
  return "ok";
}

export function validateConfig(raw: Record<string, unknown>): Record<ConfigIssueLevel, ConfigIssues> {
  const result: Record<ConfigIssueLevel, ConfigIssues> = {
    blocking: { missing: [], invalid: [] },
    warning: { missing: [], invalid: [] },
  };

  for (const rule of CONFIG_FIELD_RULES) {
    const state = checkField(readPath(raw, rule.path), rule.check);
    if (state !== "ok") result[rule.level][state].push(rule.path);
  }

  return result;
}

export type ConfigErrorReason = "load-failed" | "invalid-fields";

/**
 * Thrown by loadConfig() when the app cannot start. The message is for the
 * browser console; the UI builds its own text from `reason` and `issues`.
 */
export class ConfigError extends Error {
  constructor(
    readonly reason: ConfigErrorReason,
    message: string,
    readonly issues: ConfigIssues = NO_CONFIG_ISSUES
  ) {
    super(message);
    this.name = "ConfigError";
  }
}
