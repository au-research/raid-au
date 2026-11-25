import { Branding, ThemeConfig, BrandingHeader, BrandingFooter } from "./Appconfig";
import { neutralBranding } from "./DefaultConfig";
import { deepMerge } from "./deepMerge";

/**
 * The realm login theme that means "use the neutral branding built into the
 * app" (RAID-1065). Any other theme name means the deployment supplies its own
 * branding in app-config.json.
 */
export const DEFAULT_LOGIN_THEME = "raid-custom";

// The neutral branding is plain JSON, so a JSON round trip is a safe deep copy.
const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value));

function plainObject(value: unknown): Record<string, unknown> {
  return value !== null && typeof value === "object" && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};
}

/**
 * True when the realm uses a theme other than the default. No theme (null,
 * undefined, empty or blank) is not custom. The comparison is exact.
 */
export function isCustomLoginTheme(
  loginTheme: string | null | undefined
): loginTheme is string {
  return (
    typeof loginTheme === "string" &&
    loginTheme.trim() !== "" &&
    loginTheme !== DEFAULT_LOGIN_THEME
  );
}

/**
 * Works out the branding the app renders, from the realm's login theme and the
 * raw app-config.json. This is a pure function: it makes no network call.
 *
 * - Default theme, no theme, or a failed lookup: the neutral branding built
 *   into the app. The branding sections of the config are not read.
 * - Any other theme: the config's `theme`, `header` and `footer` sections are
 *   merged over the neutral branding field by field. Fields left out keep their
 *   neutral value, and lists replace instead of merging. A section that is not
 *   an object is ignored.
 *
 * The result is always a new object, so callers may change it freely.
 */
export function resolveBranding(
  raw: Record<string, unknown>,
  loginTheme: string | null | undefined
): Branding {
  const neutral = clone(neutralBranding);

  if (!isCustomLoginTheme(loginTheme)) {
    return neutral;
  }

  return {
    theme: deepMerge(neutral.theme, plainObject(raw.theme) as Partial<ThemeConfig>),
    header: deepMerge(neutral.header, plainObject(raw.header) as Partial<BrandingHeader>),
    footer: deepMerge(neutral.footer, plainObject(raw.footer) as Partial<BrandingFooter>),
  };
}
