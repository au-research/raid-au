// Validation rules for app-config.json (RAID-941, RAID-1065).
//
// "blocking" fields are ones the app cannot start without (no login, no API, no
// helpdesk address). "warning" fields degrade a feature when missing or wrong,
// so the app still starts and shows a banner under the header naming them.
//
// Issues only ever carry field paths, never the values found in the file.

import { FOOTER_VARIANTS, HEADER_MENUS } from "./Appconfig";
import { isHttpsUrl, isPositiveWholeNumber } from "./Organisation";

export type ConfigIssueLevel = "blocking" | "warning";

type FieldCheck = "text" | "url" | "https" | "whole-number" | "enum" | "links" | "object";

export interface FieldRule {
  path: string;
  level: ConfigIssueLevel;
  check: FieldCheck;
  /** Only checked when the field is present. A missing or blank value is ignored. */
  optional?: boolean;
  /** The allowed values for the "enum" check. */
  values?: readonly string[];
}

export const CONFIG_FIELD_RULES: readonly FieldRule[] = [
  { path: "keycloak.url", level: "blocking", check: "url" },
  { path: "keycloak.realm", level: "blocking", check: "text" },
  { path: "keycloak.clientId", level: "blocking", check: "text" },
  { path: "apiBaseUrl", level: "blocking", check: "url" },
  { path: "environment", level: "blocking", check: "text" },
  { path: "supportEmail", level: "blocking", check: "text" },
  { path: "services.orcid", level: "warning", check: "url" },
  { path: "services.staticProd", level: "warning", check: "url" },
  { path: "services.staticBase", level: "warning", check: "url" },
  { path: "app.orcid.placeholder", level: "warning", check: "text" },
  { path: "app.orcid.helpText", level: "warning", check: "text" },

  // Branding (RAID-1065). All optional: a field left out keeps its neutral
  // value. A wrong value is dropped by sanitiseBranding() and named in a banner.
  { path: "contactEmail", level: "warning", check: "text", optional: true },
  { path: "theme", level: "warning", check: "object", optional: true },
  { path: "header", level: "warning", check: "object", optional: true },
  { path: "header.title", level: "warning", check: "text", optional: true },
  { path: "header.logo", level: "warning", check: "object", optional: true },
  { path: "header.logo.light", level: "warning", check: "text", optional: true },
  { path: "header.logo.dark", level: "warning", check: "text", optional: true },
  { path: "header.logo.alt", level: "warning", check: "text", optional: true },
  { path: "header.menu", level: "warning", check: "enum", values: HEADER_MENUS, optional: true },
  { path: "header.navLinks", level: "warning", check: "links", optional: true },
  { path: "footer", level: "warning", check: "object", optional: true },
  { path: "footer.variant", level: "warning", check: "enum", values: FOOTER_VARIANTS, optional: true },
  { path: "footer.links", level: "warning", check: "links", optional: true },

  // Organisation (RAID-1066). All optional and always read, whatever the login theme.
  { path: "organisation", level: "warning", check: "object", optional: true },
  { path: "organisation.name", level: "warning", check: "text", optional: true },
  { path: "organisation.url", level: "warning", check: "https", optional: true },
  { path: "organisation.privacyPolicyUrl", level: "warning", check: "https", optional: true },
  { path: "organisation.termsUrl", level: "warning", check: "https", optional: true },
  { path: "organisation.dataDeletionDays", level: "warning", check: "whole-number", optional: true },
];

/**
 * Keys that earlier releases read and this one ignores. A file that still has
 * one gets a banner saying it is no longer used. "branding" is the old wrapper
 * around the header, footer, content and theme sections, which now sit at the
 * top level of the file.
 */
const RETIRED_KEYS = ["branding"] as const;

export interface ConfigIssues {
  missing: string[];
  invalid: string[];
  /** Keys in the file that are no longer used. Set only when there are some. */
  retired?: string[];
}

export const NO_CONFIG_ISSUES: ConfigIssues = { missing: [], invalid: [] };

export function hasConfigIssues(issues: ConfigIssues): boolean {
  return (
    issues.missing.length > 0 ||
    issues.invalid.length > 0 ||
    (issues.retired?.length ?? 0) > 0
  );
}

export function isUrlField(path: string): boolean {
  return CONFIG_FIELD_RULES.some((rule) => rule.path === path && (rule.check === "url" || rule.check === "https"));
}

type PlainObject = Record<string, unknown>;

function isPlainObject(value: unknown): value is PlainObject {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

export function readPath(raw: Record<string, unknown>, path: string): unknown {
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

/**
 * True for a link a person can safely follow: it has a label, and its address
 * is an https or mailto address. Relative paths, plain http, javascript: and
 * every other scheme are rejected.
 */
export function isSafeLink(link: unknown): boolean {
  if (!isPlainObject(link)) return false;
  const { label, path } = link;
  if (typeof label !== "string" || label.trim() === "") return false;
  if (typeof path !== "string") return false;
  try {
    const { protocol } = new URL(path);
    return protocol === "https:" || protocol === "mailto:";
  } catch {
    return false;
  }
}

export type FieldState = "ok" | "missing" | "invalid";

export function checkField(value: unknown, rule: FieldRule): FieldState {
  if (value === undefined || value === null) return rule.optional ? "ok" : "missing";
  if (rule.check === "links") {
    return Array.isArray(value) && value.every(isSafeLink) ? "ok" : "invalid";
  }
  if (rule.check === "object") return isPlainObject(value) ? "ok" : "invalid";
  if (rule.check === "whole-number") return isPositiveWholeNumber(value) ? "ok" : "invalid";
  if (typeof value !== "string") return "invalid";
  if (value.trim() === "") return rule.optional ? "ok" : "missing";
  if (rule.check === "url" && !isHttpUrl(value)) return "invalid";
  if (rule.check === "https" && !isHttpsUrl(value.trim())) return "invalid";
  if (rule.check === "enum" && !rule.values?.includes(value)) return "invalid";
  return "ok";
}

export function validateConfig(raw: Record<string, unknown>): Record<ConfigIssueLevel, ConfigIssues> {
  const result: Record<ConfigIssueLevel, ConfigIssues> = {
    blocking: { missing: [], invalid: [] },
    warning: { missing: [], invalid: [] },
  };

  for (const rule of CONFIG_FIELD_RULES) {
    const state = checkField(readPath(raw, rule.path), rule);
    if (state !== "ok") result[rule.level][state].push(rule.path);
  }

  const retired = RETIRED_KEYS.filter((key) => raw[key] !== undefined && raw[key] !== null);
  if (retired.length > 0) {
    result.warning.retired = [...retired];
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
